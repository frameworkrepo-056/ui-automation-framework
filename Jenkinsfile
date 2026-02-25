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
            description: 'Cucumber tag filter e.g. @smoke (leave empty to run all)'
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

                // Allure Jenkins Plugin — renders interactive report in Jenkins UI
                allure([
                    includeProperties: true,
                    jdk: '',
                    results: [[path: 'target/allure-results']]
                ])

                // Zip the HTML report using Windows PowerShell
                bat """
                    powershell -Command "Compress-Archive -Path target\\allure-report\\* -DestinationPath target\\allure-report-${BUILD_NUMBER}.zip -Force"
                """

                // Archive the zip as a downloadable artifact
                archiveArtifacts(
                    artifacts: "target/allure-report-${BUILD_NUMBER}.zip",
                    fingerprint: true,
                    allowEmptyArchive: false
                )

                // Archive logs
                archiveArtifacts(
                    artifacts: 'target/logs/*.log',
                    fingerprint: true,
                    allowEmptyArchive: true
                )
            }
        }
    }

    post {

        success {
            echo "✅ BUILD SUCCESS — All tests passed"
        }

        failure {
            echo "❌ BUILD FAILED — Check Allure report for failures"
        }

        unstable {
            echo "⚠️ BUILD UNSTABLE — Some tests failed"
        }

        always {
            // cleanWs must be inside node context — agent any provides it
            node('built-in') {
                cleanWs(
                    cleanWhenSuccess: false,
                    cleanWhenFailure: false,
                    cleanWhenAborted: true
                )
            }
        }
    }
}