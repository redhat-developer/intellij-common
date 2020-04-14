#!/usr/bin/env groovy

node('rhel7'){
	stage('Checkout repo') {
		deleteDir()
		git url: 'https://github.com/redhat-developer/intellij-common',
			branch: "${sha1}"
	}

	stage('Build') {
		sh "./gradlew assemble"
	}

	stage('Package') {
        sh "./gradlew build"
	}

	stage('Deploy') {
		withCredentials([usernamePassword(credentialsId: 'Nexus-IJ-Credentials', usernameVariable: 'USER', passwordVariable: 'PASSWORD')]) {
			sh "./gradlew publish -PnexusUser=${USER} -PnexusPassword=${PASSWORD}"
		}
	}

}
