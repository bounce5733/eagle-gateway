BIN_HOME=`dirname $0`
cd $BIN_HOME
BIN_HOME=`pwd`

JAVA_BIN=/opt/jdk/bin
APP_NAME=gateway-server
JAR_NAME=${APP_NAME}.jar
JAVA_OPTS="-Xms256m -Xmx512m"

CONFIG_ADDRESS=127.0.0.1:8848
CONFILE_FILES=gateway_server.yaml,app_route.yaml
CONFIG_PREFIX=spring.cloud.nacos.config
JAVA_CONF_OPTS=\
"-D${CONFIG_PREFIX}.namespace=cc4a-sfdsf--sfsdfsdf--sdfsf \
-D${CONFIG_PREFIX}.server-addr=${CONFIG_ADDRESS} \
-D${CONFIG_PREFIX}.shared-dataids=${CONFIG_FILES} \
-D${CONFIG_PREFIX}.refreshable-dataids=${CONFIG_FILES}"

start(){
  pid=`ps -ef|grep ${JAR_NAME} | grep -v grep |awk '{print $2}'`
  if [ -n "${pid}" ];then
    echo "程序已经启动，进程号：${pid}"
  else
    echo -n "开始启动程序..."
    nohup ${JAVA_BIN}/java ${JAVA_OPTS} ${JAVA_CONF_OPTS} -jar ${JAR_NAME} > /dev/null 2>&1 &

    sleep 2 #防止错误的进程状态（启动成功，但程序马上停止的情况）

    pid=`ps -ef|grep ${JAR_NAME} | grep -v grep |awk '{print $2}'`

    if [ -n "${pid}" ];then
      echo "启动成功，进程号：${pid}"
    else
      echo "启动失败"
    fi
  fi
}

stop(){
  pid=`ps -ef|grep ${JAR_NAME} | grep -v grep |awk '{print $2}'`
  if [ -n "${pid}" ];then
    kill -9 ${pid}
    echo "成功停止进程：${pid}"
  else
    echo "${APP_NAME}进程不存在..."
  fi
}

restart(){
  stop
  start
}

status(){
  pid=`ps -ef|grep ${JAR_NAME} | grep -v grep |awk '{print $2}'`
  if [ -n "${pid}" ];then
    echo "程序${APP_NAME}运行中，进程号：${pid}"
  else
    echo "未找到${APP_NAME}进程"
  fi
}

case "$1" in
  'start')
  start
  ;;
  'stop')
  stop
  ;;
  'restart')
  restart
  ;;
  'status')
  status
  ;;
  *)

echo "usage: $0 {start|stop|restart|status}"
exit 1
esac
exit 0