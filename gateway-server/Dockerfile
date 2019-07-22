FROM 192.168.3.8/pub/openjdk8

ADD target/*.jar app.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom" ,"-Dapp.id=eagle-gateway","-Denv=DEV","-Dapollo.meta=http://192.168.3.8:8080","-Dapollo.configService=http://192.168.3.8:8080","-Dapollo.bootstrap.enabled=true","-Dapollo.bootstrap.namespaces=gateway-server.yaml,gateway-base.yaml","-jar", "-Xms256m","-Xmx256m" ,"/app.jar"]