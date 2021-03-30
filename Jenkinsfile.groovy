pipeline {

    agent any
    environment {
        ANSIBLE_HOST_KEY_CHECKING = 'false'
    	VERSION = "0.1"
        //AWS_REGION = "us-east-1"
        //AWS_CREDENTIAL = "aws-credentials-jenkins-s3"
        //S3_ARTIFACT = "semperti-rapientrega-development-s3-backend-artifact"
        //S3_ARTIFACT_OBJECT = "rapientrega.zip"
    }
    stages {
        stage('Stage 1 - Configure & Clean Slave') {
            steps {
                echo "STAGE1 - Tasks pre Test and build"
                //sh "git clone https://github.com/gonzaloacosta/springboot-apirest app"
                sh "ls -ltr"
            }
        }
        stage('Stage 2 - Unit Test') {
            steps {
                echo "STAGE2 - Unit Test"
                sh "pwd ; ls -ltr"
                sh "mvn test"
            }
        }
        stage('Stage 3 - Release & Package') {
            steps {
                echo "STAGE3 - Release & Upload Nexus"
                sh "mvn versions:set -DnewVersion=$env.VERSION"
                sh "mvn clean package -DskipTests"
            }
        }
        stage('Stage 4 - Create Artifact') {
            steps {
                echo "STAGE 4 - Create Artifact"
                sh "echo test > serco.crt"
                zip archive: true, dir: 'archive', glob: 'Dockerfile, serco.crt, target/*.jar', zipFile: 'rapientrega.zip'
            }
        }
        stage('Stage 5 - Upload Artifact to S3 Bucket') {
            steps {
                echo "STAGE 5 - Upload Artifact to S3 Bucket"
                withAWS(credentials: "aws-credentials-jenkins-s3", region: "us-east-1") {
                    s3Upload(file:"rapientrega.zip", 
                    bucket:"semperti-rapientrega-development-s3-backend-artifact", 
                    path:"rapientrega.zip")
                }
            }
        }
        stage('Stage 6 - Docker pull & run') {
            steps {
                echo "STAGE 6 - Docker pull & run"
                //dir("${env.WORKSPACE}/ansible"){
                //    sh "pwd"
                //    sh "ansible-playbook --extra-vars @vars/ansible-vars.json stage6-docker-run.yml -e VERSION=$env.VERSION"
                //}
                //timeout(300) {
                //    waitUntil {
                //        script {
                //            def r = sh script: 'curl http://localhost:8080', returnStatus: true
                //            return (r == 0);
                //        }
                //    }
                //} 
            }
        }
        stage('Stage 7 - Check Application RUN') {
            steps {
                echo 'STAGE 7 - Check Application RUN'
                //steps {
                //    sh "curl http://localhost:8080"
                //}
            }
        }
    }
}
