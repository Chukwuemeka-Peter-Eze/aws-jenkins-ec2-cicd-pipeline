pipeline {

    agent any

    stages {

        stage('build jar') {
            steps {
                echo 'building the application...'

                sh 'mvn package'
            }
        }

        stage('build image') {
            steps {
                echo 'building the docker image...'

                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker-hub-repo',
                        passwordVariable: 'PASS',
                        usernameVariable: 'USER'
                    )
                ]) {

                    sh 'docker build -t pierrechukason/demo-app.jma-1.1 .'

                    sh 'echo $PASS | docker login -u $USER --password-stdin'

                    sh 'docker push pierrechukason/demo-app.jma-1.1'
                }
            }
        }

        stage('deploy') {
            steps {
                echo 'deploying the application...'

                sshagent(credentials: ['ec2-server-key']) {

                    sh '''
                        ssh -o StrictHostKeyChecking=no ubuntu@54.209.6.238 "
                            docker pull pierrechukason/demo-app.jma-1.1
                            docker stop demo-app 2>/dev/null || true
                            docker rm demo-app 2>/dev/null || true
                            docker run -d --name demo-app -p 8080:8080 pierrechukason/demo-app.jma-1.1
                        "
                    '''
                }
            }
        }
    }
}