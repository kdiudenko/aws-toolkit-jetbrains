// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.kinesis

import com.intellij.openapi.project.Project
import software.aws.toolkits.jetbrains.core.explorer.nodes.AwsExplorerNode
import software.aws.toolkits.jetbrains.core.explorer.nodes.AwsExplorerServiceNode
import software.aws.toolkits.jetbrains.core.explorer.nodes.AwsExplorerServiceRootNode
import software.aws.toolkits.resources.message

class KinesisServiceNode(project: Project, service: AwsExplorerServiceNode): AwsExplorerServiceRootNode(project, service) {
    override fun displayName(): String = message("explorer.node.kinesis")

    override fun getChildrenInternal(): List<AwsExplorerNode<*>> {
        return emptyList()
    }
}
