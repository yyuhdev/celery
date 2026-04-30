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
                checkout([$class: 'GitSCM',
                    branches: [[name: "*/v3"]],
                    userRemoteConfigs: scm.userRemoteConfigs,
                    extensions: [
                        [$class: 'SubmoduleOption',
                            recursiveSubmodules: true,
                            parentCredentials: true,
                            trackingSubmodules: false
                        ]
                    ]
                ])
            }
        }

        stage('Setup Buf') {
            steps {
                sh '''
                set -e

                BUF_VERSION=1.66.1

                curl -sSL \
                https://github.com/bufbuild/buf/releases/download/v${BUF_VERSION}/buf-Linux-x86_64 \
                -o /usr/local/bin/buf

                chmod +x /usr/local/bin/buf

                /usr/local/bin/buf --version
                '''
            }
        }

        stage('Integration Testing') {
            steps {
                sh './gradlew testing:test --no-daemon'
            }
        }
    }
}
