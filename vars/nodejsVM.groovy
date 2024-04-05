def call(Map configMap) {
pipeline {
   agent {
    node {
        label 'agent1'
    }
    }
    options {
        timeout(time: 1, unit: 'HOURS') 
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    environment {
        packageVersion = ''
        nexusUrl= "172.31.92.65:8081"

        }
    
    // parameters {
    //     string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')

    //     text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')

        booleanParam(name: 'Deploy', defaultValue: true, description: 'Toggle this value')

    //     choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')

    //     password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
    
    stages {
        stage('getting version') {
            steps {
                script // we are script because this is ruby scripting
                {
                    def packageJson = readJSON file: 'catalogue/package.json'
                    packageVersion = packageJson.version
                    echo "package  version :  $packageVersion "
                }
            }
        }
        stage('dependencies') {
            steps {
                sh """
                cd catalogue
                npm install
                """
            }
        }
        stage('build') {
            steps {
                sh """
                cd catalogue
                ls -la
                zip -q -r ${configMap.component}.zip ./* -x ".git" -x "*.zip"
                 ls -ltr
                """
            }
        }
        stage('PublishArtifact') {
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: "${nexusURL}",
                    groupId: 'com.roboshop',
                    version: "${packageVersion}",
                    repository:  "${configMap.component}",
                    credentialsId: 'nexus_auth',
                    artifacts: [
                        [artifactId: "${configMap.component}",
                        classifier: '',
                        file: "${configMap.component}/${configMap.component}.zip",
                        type: 'zip']
                     ]
                    )
                   }
                }

    

        stage('trigger another pipeline') {
            when {
                expression [
                    params.Deploy
                ]
            }
            steps {
                script{
                //     def  params = [
                //     string(name: 'version', value: "$packageVersion"),
                //     string(name: 'environment', value: 'dev') ]
                //   build job: 'catalogue_deploy', wait:true parameters: params
                  def params = [
                            string(name: 'version', value: "$packageVersion"),
                            string(name: 'environment', value: "dev")
                        ]
                        build job: "catalogue_deploy", wait: true, parameters: params
                }
            }
        }
        
    }
     post { 
        always { 
            echo 'I will always say Hello again!'
             deleteDir()
        }
     }
        
}
    }