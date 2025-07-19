#!/usr/bin/env groovy
import java.time.LocalDate

//--- Config App ---
final LocalDate now = LocalDate.now()
def APP_NAME = "pos-mcp-server"
def NAMESPACES = "apps"
def DEPLOYMENT_IMAGE_TAG = "registry.gitlab.com/posapi/${APP_NAME}:0.0.1-SNAPSHOT"
def IMAGE_TAG = "registry.gitlab.com/posapi/${APP_NAME}:$env.BUILD_NUMBER'.'${now.dayOfMonth}'.'${now.monthValue}'.'${now.year}"
def BRANCH_MAP = [test: 'test', develop: 'qa', master: 'prod']

//--- Credentials Id de Jenkins Global Credentials ---
def CREDENTIALS_ID_REGISTRY_URL = "registry-url"
def CREDENTIALS_ID_USER_PASS_REGISTRY = "registry-username-password"
def CREDENTIALS_ID_KUBE_CONFIG = "kubeconfig"

//--Configuracion de Gradle cache
def CACHE_DIR = "/gradle/cache/${APP_NAME}"

podTemplate(yaml: '''
kind: Pod
metadata:
  name: jenkins-agent
spec:
  containers:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    env:
      - name: container
        value: docker
    tty: true
    command:
      - sleep
    args:
      - 9999999
  - name: kubectl
    image: lachlanevenson/k8s-kubectl:v1.15.9
    tty: true
    command:
    - cat
  - name: openjdk
    image: eclipse-temurin:17.0.9_9-jdk
    command:
    - cat
    tty: true
    env:
      - name: GRADLE_USER_HOME
        value: "${CACHE_DIR}/.gradle"
    volumeMounts:
      - name: m2-repository
        mountPath: "${CACHE_DIR}/.gradle/caches/jars-9/"
  volumes:
  - name: m2-repository
    persistentVolumeClaim:
      claimName: m2-repository-pvc-claim
'''
  ) {

node(POD_LABEL) {
      // ------------------------------------
      // -- ETAPA: Checkout en el proyecto y rama indicada
      // ------------------------------------
      stage('Checkout SCM') {
         echo 'Checkout proyecto'
         checkout scm
      }

      // ------------------------------------
      // -- ETAPA: Test
      // ------------------------------------
      stage('Test') {
         echo 'Ejecutando tests'
         //container('openjdk') {
            sh './gradlew test'
         //}
      }

      // ------------------------------------
      // -- ETAPA: Instalar
      // ------------------------------------
      stage('Instalar') {
         echo 'Instala el paquete generado en el repositorio gradle'
         container('openjdk') {
            sh './gradlew clean build -x test'
         }
      }

	  // ------------------------------------
      // -- ETAPA: Generar Imagen Docker
      // ------------------------------------
      stage('Generar imagen Docker') {
         echo 'Genera Imagen Docker'
         container('kaniko') {
         	withCredentials([usernamePassword(credentialsId: "${CREDENTIALS_ID_USER_PASS_REGISTRY}", usernameVariable: 'CI_REGISTRY_USER', passwordVariable: 'CI_REGISTRY_PASSWORD'),
         					string(credentialsId: "${CREDENTIALS_ID_REGISTRY_URL}", variable: 'CI_REGISTRY')]) {
	         	sh "mkdir -p /kaniko/.docker"
	         	sh """
	         		echo "{\\"auths\\":{\\"$CI_REGISTRY\\":{\\"auth\\":\\"\$(printf "%s:%s" "$CI_REGISTRY_USER" "$CI_REGISTRY_PASSWORD" | base64 | tr -d '\n')\\"}}}" > /kaniko/.docker/config.json
	         		"""
	         	sh "cat /kaniko/.docker/config.json"
	            sh "/kaniko/executor -f `pwd`/Dockerfile -c `pwd` --destination=${IMAGE_TAG}"
            }
         }
      }

      // ------------------------------------
      // -- ETAPA: Correr imagen Docker
      // ------------------------------------
      stage('Correr imagen Docker') {
         echo 'Correr imagen Docker'
		 container('kubectl') {
		 	  withCredentials([file(credentialsId: "${CREDENTIALS_ID_KUBE_CONFIG}", variable: 'KUBECONFIG')]) {
		 		      //sh "kubectl create namespace ${NAMESPACES} 2>/dev/null" Se comenta mientras se resuelve cuando existe el namespaces
	            sh "sed -i 's#${DEPLOYMENT_IMAGE_TAG}#${IMAGE_TAG}#g' k8s/deployment.yaml"
	            sh 'kubectl apply -f k8s/deployment.yaml'
         	}
		    }
      }
}
}