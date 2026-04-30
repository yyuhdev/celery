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

        stage('Setup') {
            steps {
              sh '''
                set -e
                curl -sSL https://github.com/bufbuild/buf/releases/latest/download/buf-Linux-x86_64 -o buf
                chmod +x buf
                export PATH=$PWD:$PATH
              '''
            }
        }

        stage('Integration Testing') {
            steps {
                sh 'gradle testing:test'
            }
        }
    }
}
