// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.aws.toolkits.jetbrains.services.sqs.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.PopupHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.aws.toolkits.jetbrains.services.sqs.MAX_NUMBER_OF_POLLED_MESSAGES
import software.aws.toolkits.jetbrains.services.sqs.Queue
import software.aws.toolkits.jetbrains.services.sqs.actions.DeleteMessageAction
import software.aws.toolkits.jetbrains.utils.ApplicationThreadPoolScope
import software.aws.toolkits.resources.message
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class PollMessagePane(
    private val project: Project,
    private val client: SqsClient,
    private val queue: Queue
) : CoroutineScope by ApplicationThreadPoolScope("PollMessagesPane") {
    lateinit var component: JPanel
    lateinit var messagesAvailableLabel: JLabel
    lateinit var tablePanel: SimpleToolWindowPanel
    lateinit var pollButton: JButton
    val messagesTable = MessagesTable()

    private fun createUIComponents() {
        tablePanel = SimpleToolWindowPanel(false, true)
    }

    init {
        tablePanel.setContent(PollWarning(this).content)
        pollButton.isVisible = false
        pollButton.addActionListener {
            poll()
        }
        addActionsToTable()
    }

    fun startPolling() {
        tablePanel.setContent(messagesTable.component)
        pollButton.isVisible = true
        launch {
            requestMessages()
            addTotal()
        }
    }

    suspend fun requestMessages() {
        try {
            withContext(Dispatchers.IO) {
                val polledMessages: List<Message> = client.receiveMessage {
                    it.queueUrl(queue.queueUrl)
                    it.attributeNames(QueueAttributeName.ALL)
                    it.maxNumberOfMessages(MAX_NUMBER_OF_POLLED_MESSAGES)
                }.messages()

                polledMessages.forEach {
                    messagesTable.tableModel.addRow(it)
                }
                messagesTable.setBusy(busy = false)
            }
        } catch (e: Exception) {
            messagesTable.table.emptyText.text = message("sqs.failed_to_poll_messages")
        }
    }

    suspend fun addTotal() {
        try {
            withContext(Dispatchers.IO) {
                val numMessages = client.getQueueAttributes {
                    it.queueUrl(queue.queueUrl)
                    it.attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                }.attributes().getValue(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)

                messagesAvailableLabel.text = message("sqs.messages.available.text") + numMessages
            }
        } catch (e: Exception) {
            messagesAvailableLabel.text = message("sqs.failed_to_load_total")
        }
    }

    private fun addActionsToTable() {
        val actionGroup = DefaultActionGroup().apply {
            add(DeleteMessageAction(project, client, messagesTable.table, queue.queueUrl))
        }
        PopupHandler.installPopupHandler(
            messagesTable.table,
            actionGroup,
            ActionPlaces.EDITOR_POPUP,
            ActionManager.getInstance()
        )
    }

    private fun poll() = launch {
        // TODO: Add debounce
        messagesTable.setBusy(busy = true)
        messagesTable.reset()
        requestMessages()
        addTotal()
    }
}
