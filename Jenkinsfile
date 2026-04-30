pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: gradle
    image: gradle:jdk-25-and-25
    command: ['sleep', '999999']
"""
            defaultContainer 'gradle'
        }
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Integration Testing') {
            steps {
                sh 'gradle testing:test'
            }
        }
    }
}
