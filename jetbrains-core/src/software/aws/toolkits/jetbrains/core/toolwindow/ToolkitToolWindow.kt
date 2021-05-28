// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.impl.ContentImpl
import icons.AwsIcons
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon
import javax.swing.JComponent

interface ToolkitToolWindow {
    fun addTab(
        title: String,
        component: JComponent,
        activate: Boolean = false,
        id: String = title,
        disposable: Disposable? = null,
        refresh: () -> Unit = { }
    ): ToolkitToolWindowTab

    fun find(id: String): ToolkitToolWindowTab?

    // prefix is prefix of the id. Assumes the window is using id composed of paths, like: "loggroup/logstream"
    fun findPrefix(prefix: String): List<ToolkitToolWindowTab>
}

interface ToolkitToolWindowTab : Disposable {
    val name: String
    fun show(refresh: Boolean = false)
}

class ToolkitToolWindowManager(private val project: Project) : Disposable {
    private val toolWindows = ConcurrentHashMap<ToolkitToolWindowType, ManagedToolkitToolWindow>()
    internal fun getInstance(type: ToolkitToolWindowType) = toolWindows.computeIfAbsent(type) { ManagedToolkitToolWindow(type) }

    inner class ManagedToolkitToolWindow(private val type: ToolkitToolWindowType) : ToolkitToolWindow {
        private val tabs = mutableMapOf<String, ManagedToolkitToolWindowTab>()

        override fun addTab(
            title: String,
            component: JComponent,
            activate: Boolean,
            id: String,
            disposable: Disposable?,
            refresh: () -> Unit
        ): ToolkitToolWindowTab {
            val content = ContentImpl(component, title, true)
            val toolWindow = windowManager.getToolWindow(type.id)
                ?: windowManager.registerToolWindow(type.id, true, type.anchor, this@ToolkitToolWindowManager, true).also {
                    it.setIcon(type.icon)
                    it.stripeTitle = type.title
                }
            Disposer.register(content, Disposable { closeWindowIfEmpty(toolWindow, type.id) })
            disposable?.let { Disposer.register(content, it) }
            toolWindow.contentManager.addContent(content)
            return ManagedToolkitToolWindowTab(toolWindow, content, refresh).also {
                tabs[id] = it
                if (activate) {
                    it.show()
                }
            }
        }

        override fun find(id: String): ToolkitToolWindowTab? {
            val tab = tabs[id] ?: return null
            if (Disposer.isDisposed(tab.content)) {
                tabs.remove(id)
                return null
            }
            return tab
        }

        override fun findPrefix(prefix: String): List<ToolkitToolWindowTab> = tabs
            .filter { it.key.startsWith("$prefix/") || it.key == prefix }
            .mapNotNull {
                if (Disposer.isDisposed(it.value.content)) {
                    tabs.remove(it.key)
                    null
                } else {
                    it.value
                }
            }
    }

    class ManagedToolkitToolWindowTab(private val toolWindow: ToolWindow, internal val content: Content, private val refresh: () -> Unit) :
        ToolkitToolWindowTab {
        override val name: String = content.displayName

        override fun show(refresh: Boolean) {
            toolWindow.activate(null, true)
            toolWindow.contentManager.setSelectedContent(content)
            if (refresh) {
                refresh()
            }
        }

        override fun dispose() {
            if (!Disposer.isDisposed(content)) {
                toolWindow.contentManager.removeContent(content, true)
            }
        }
    }

    private val windowManager
        get() = ToolWindowManager.getInstance(project)

    private fun closeWindowIfEmpty(window: ToolWindow, id: String) {
        if (window.contentManager.contentCount == 0) {
            windowManager.unregisterToolWindow(id)
        }
    }

    override fun dispose() {
    }

    companion object {
        fun getInstance(project: Project, toolWindowType: ToolkitToolWindowType): ToolkitToolWindow = ServiceManager.getService(
            project,
            ToolkitToolWindowManager::class.java
        ).getInstance(toolWindowType)
    }
}

data class ToolkitToolWindowType(
    val id: String,
    val title: String,
    val icon: Icon = AwsIcons.Logos.AWS,
    val anchor: ToolWindowAnchor = ToolWindowAnchor.BOTTOM
)
