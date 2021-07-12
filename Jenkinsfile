pipeline {
    agent any

    triggers {
        pollSCM('* * * * *') //polling for changes once a minute
    }

    stages {
        stage('Build') {
            steps {
            	echo 'Building...'
                sh './gradlew clean build'

                archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
                archiveArtifacts artifacts: '**/Dockerfile', fingerprint: true
            }
        }
        stage('Release') {
            steps {
                echo 'Building release...'
                sh './gradlew :app:assembleRelease'
                echo 'Storing apk file for publishing'
                archiveArtifacts artifacts: '**/app/build/outputs/apk/release', fingerprint: true
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying on server...'
                script {
                	def remote =[:]
					remote.name = 'webserver'
					remote.host = '178.62.194.237'
					remote.user = 'jenkins'
					remote.password = 'Duco2020#1'
					remote.allowAnyHosts = true
                    sshPut remote: remote, from: 'app/build/outputs/apk/release', into: '/var/www/html/app'
                }
                echo 'Deploy SUCCESSFULL'
            }
        }
    }
}
