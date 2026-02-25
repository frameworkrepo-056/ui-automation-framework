pipeline {

    agent any

    tools {
        jdk   'JDK-25'
        maven 'Maven-3'
    }

    parameters {
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'edge'],
            description: 'Browser to run tests on'
        )
        string(
            name: 'TAGS',
            defaultValue: '',
            description: 'Cucumber tag filter e.g. @smoke, @regression (leave empty to run all)'
        )
        string(
            name: 'THREADS',
            defaultValue: '4',
            description: 'Number of parallel threads'
        )
        booleanParam(
            name: 'HEADLESS',
            defaultValue: true,
            description: 'Run in headless mode'
        )
    }

    environment {
        ALLURE_RESULTS = "${WORKSPACE}/target/allure-results"
        TIMESTAMP      = sh(script: 'date +%Y%m%d_%H%M%S', returnStdout: true).trim()
    }

    stages {

        stage('Checkout') {
            steps {
                echo "Checking out development branch..."
                git branch: 'development',
                    url: 'https://github.com/frameworkrepo-056/ui-automation-framework.git'
            }
        }

        stage('Build') {
            steps {
                echo "Compiling project..."
                bat 'mvn clean compile test-compile -q'
            }
        }

        stage('Test') {
            steps {
                echo "Running tests: Browser=${params.BROWSER} | Headless=${params.HEADLESS} | Threads=${params.THREADS} | Tags=${params.TAGS}"
                script {
                    def tagFilter = params.TAGS?.trim()
                        ? "-Dcucumber.filter.tags=\"${params.TAGS}\""
                        : ''

                    bat """
                        mvn test ^
                            -Dbrowser=${params.BROWSER} ^
                            -Dheadless=${params.HEADLESS} ^
                            -Dparallel.threads=${params.THREADS} ^
                            ${tagFilter}
                    """
                }
            }
        }

        stage('Generate Allure Report') {
            steps {
                echo "Generating Allure report..."
                bat 'mvn allure:report -q'
            }
        }

        stage('Archive Reports') {
            steps {
                echo "Archiving reports..."

                // Archive Allure results (raw JSON — used by Allure Jenkins Plugin)
                allure([
                    includeProperties: true,
                    jdk: '',
                    results: [[path: 'target/allure-results']]
                ])

                // Archive full HTML report as zip
                bat """
                    powershell Compress-Archive ^
                        -Path target\\allure-report\\* ^
                        -DestinationPath target\\allure-report-${BUILD_NUMBER}.zip ^
                        -Force
                """

                archiveArtifacts artifacts: "target/allure-report-${BUILD_NUMBER}.zip",
                                 fingerprint: true,
                                 allowEmptyArchive: false

                // Archive logs
                archiveArtifacts artifacts: 'target/logs/*.log',
                                 fingerprint: true,
                                 allowEmptyArchive: true
            }
        }
    }

    post {

        success {
            echo "✅ BUILD SUCCESS — All tests passed"
            echo "Allure report available in the build page"
        }

        failure {
            echo "❌ BUILD FAILED — Check Allure report for failures"
        }

        unstable {
            echo "⚠️ BUILD UNSTABLE — Some tests failed"
        }

        always {
            echo "Cleaning up workspace..."
            cleanWs(
                cleanWhenSuccess: false,
                cleanWhenFailure: false,
                cleanWhenAborted: true
            )
        }
    }
}