FROM amazoncorretto:11 as build
ARG GRADLE_BUILD_TASK
ARG GRADLE_BUILD_OUTPUT
RUN yum -y install git
RUN git clone https://github.com/sitmun/sitmun-backend-core.git --depth 1
WORKDIR /sitmun-backend-core
RUN ./gradlew $GRADLE_BUILD_TASK
RUN mv $GRADLE_BUILD_OUTPUT sitmun.jar

FROM amazoncorretto:11
COPY --from=build /sitmun-backend-core/sitmun.jar sitmun.jar
ENTRYPOINT ["java", "-Dserver.port=8080", "-jar","sitmun.jar"]
EXPOSE 8080
