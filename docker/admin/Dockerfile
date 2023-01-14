FROM node:12 as build
ARG GITHUB_TOKEN
ARG BASE_HREF
RUN apt-get update
RUN apt-get -y install git
RUN git clone https://github.com/sitmun/sitmun-admin-app.git --depth 1
WORKDIR /sitmun-admin-app
RUN git checkout
RUN npm set //npm.pkg.github.com/:_authToken $GITHUB_TOKEN
RUN npm ci
COPY environment.testdeployment.ts src/environments
RUN npm run build -- --configuration=testdeployment --baseHref="$BASE_HREF"

FROM nginx:alpine
WORKDIR /app
COPY --from=build /sitmun-admin-app/dist/admin-app static
COPY ./nginx.conf /etc/nginx/nginx.conf
EXPOSE 8080