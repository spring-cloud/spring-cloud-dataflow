FROM tomcat:8-jre8-alpine

ENV UAA_CONFIG_PATH=$CATALINA_HOME
ENV CATALINA_OPTS="-Xmx800m"

RUN rm -fr $CATALINA_HOME/webapps/ROOT
RUN rm -f $CATALINA_HOME/webapps/ROOT.war
RUN sed -i 's/port="8080"/port="8099"/' ${CATALINA_HOME}/conf/server.xml
ADD cloudfoundry-identity-uaa.war $CATALINA_HOME/webapps/uaa.war
ADD uaa.yml $CATALINA_HOME/uaa.yml

EXPOSE 8099

CMD ["catalina.sh", "run"]

