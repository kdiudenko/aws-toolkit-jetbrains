// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import software.aws.toolkits.gradle.IdeVersions
import software.aws.toolkits.gradle.changelog.tasks.GenerateGithubChangeLog

val ideProfile = IdeVersions.ideProfile(project)
val toolkitVersion: String by project

plugins {
    id("base")
    id("toolkit-changelog")
    id("toolkit-jacoco-report")
}

allprojects {
    repositories {
        mavenLocal()
        System.getenv("CODEARTIFACT_URL")?.let {
            println("Using CodeArtifact proxy: $it")
            maven {
                url = uri(it)
                credentials {
                    username = "aws"
                    password = System.getenv("CODEARTIFACT_AUTH_TOKEN")
                }
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

tasks.register<GenerateGithubChangeLog>("generateChangeLog") {
    changeLogFile.set(project.file("CHANGELOG.md"))
}

tasks.createRelease.configure {
    releaseVersion.set(providers.gradleProperty("toolkitVersion"))
}

dependencies {
    aggregateCoverage(project(":intellij"))
    aggregateCoverage(project(":ui-tests"))
}

tasks.register("runIde") {
    doFirst {
        throw GradleException("Use project specific runIde command, i.e. :jetbrains-core:runIde, :intellij:runIde")
    }
}
