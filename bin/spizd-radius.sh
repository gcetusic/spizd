DIR=`dirname $0`
#java -cp $DIR/../lib/TinyRadius-1.0.jar:$DIR/../lib/commons-logging.jar:$DIR/../lib/log4j-1.2.8.jar:$DIR/../lib/mail-1.4.jar:$DIR/../dist/spizd.jar com.nimium.spizd.RADIUSConnector $1 $2
java -cp $DIR/../lib/TinyRadius-1.0.jar:$DIR/../lib/mail-1.4.jar:$DIR/../dist/spizd.jar com.nimium.spizd.RADIUSConnector $1 $2
