#!/usr/bin/env groovy

node('rhel7'){
	stage('Checkout repo') {
		deleteDir()
		git url: 'https://github.com/redhat-developer/intellij-common',
			branch: "${sha1}"
	}

	def props = readProperties file: 'gradle.properties'
	def isSnapshot = props['projectVersion'].contains('-SNAPSHOT')
	def version = isSnapshot?props['projectVersion'].replace('-SNAPSHOT', ".${env.BUILD_NUMBER}"):props['projectVersion'] + ".${env.BUILD_NUMBER}"

	stage('Build') {
		sh "./gradlew assemble"
	}

	stage('Package') {
        sh "./gradlew build"
	}

	stage('Deploy') {
		withCredentials([usernamePassword(credentialsId: 'Nexus-IJ-Credentials', usernameVariable: 'USER', passwordVariable: 'PASSWORD')]) {
			sh "./gradlew build publish -PnexusUser=${USER} -PnexusPassword=${PASSWORD}"
		}
	}

}
