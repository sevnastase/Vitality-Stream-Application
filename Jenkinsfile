pipeline {
    agent any

    triggers {
        pollSCM('* * * * *') //polling for changes once a minute
    }

    stages {
        stage('Dry Build') {
            steps {
            	echo 'Building...'
                sh './gradlew clean build --no-daemon'
            }
        }
        stage('Release') {
            steps {
                echo 'Building release...'
                sh './gradlew :app:assembleRelease --no-daemon'
                echo 'Storing apk file for publishing'
                archiveArtifacts artifacts: '**/app/build/outputs/apk/release/*', fingerprint: true
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
					sshRemove remote: remote, path: "/var/www/html/app/app-release.apk"
					sshRemove remote: remote, path: "/var/www/html/app/output-metadata.json"
                    sshPut remote: remote, from: 'app/build/outputs/apk/release/app-release.apk', into: '/var/www/html/app/'
                    sshPut remote: remote, from: 'app/build/outputs/apk/release/output-metadata.json', into: '/var/www/html/app/'
                }
                echo 'Deploy SUCCESSFULL'
            }
        }
    }
}
