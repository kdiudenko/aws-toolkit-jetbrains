// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import software.aws.toolkits.jetbrains.core.explorer.DeleteResourceDialog
import software.aws.toolkits.jetbrains.core.explorer.nodes.AwsExplorerResourceNode
import software.aws.toolkits.jetbrains.utils.notifyError
import software.aws.toolkits.jetbrains.utils.notifyInfo
import software.aws.toolkits.resources.message
import software.aws.toolkits.telemetry.AwsTelemetry
import software.aws.toolkits.telemetry.Result

abstract class DeleteResourceAction<in T : AwsExplorerResourceNode<*>>(text: String) :
    SingleResourceNodeAction<T>(text, icon = AllIcons.Actions.Cancel), DumbAware {
    final override fun actionPerformed(selected: T, e: AnActionEvent) {
        val resourceType = selected.resourceType()
        val resourceName = selected.displayName()
        val response = DeleteResourceDialog(selected.nodeProject, resourceType, resourceName).showAndGet()
        if (!response) {
            AwsTelemetry.deleteResource(selected.project, selected.serviceId, Result.Cancelled)
        } else {
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    performDelete(selected)
                    notifyInfo(project = selected.project, title = message("delete_resource.deleted", resourceType, resourceName))
                    AwsTelemetry.deleteResource(selected.project, selected.serviceId, success = true)
                } catch (e: Exception) {
                    e.notifyError(project = selected.project, title = message("delete_resource.delete_failed", resourceType, resourceName))
                    AwsTelemetry.deleteResource(selected.project, selected.serviceId, success = false)
                }
            }
        }
    }

    abstract fun performDelete(selected: T)
}
