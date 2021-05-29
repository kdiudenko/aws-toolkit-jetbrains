// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.explorer.nodes

import com.intellij.openapi.project.Project
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.aws.toolkits.jetbrains.services.kinesis.KinesisServiceNode

class KinesisExplorerRootNode : AwsExplorerServiceNode {
    override val serviceId: String = KinesisClient.SERVICE_NAME
    override fun buildServiceRootNode(project: Project) = KinesisServiceNode(project, this)
}
