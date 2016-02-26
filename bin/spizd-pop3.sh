DIR=`dirname $0`
java -cp $DIR/../lib/mail-1.4.jar:$DIR/../dist/spizd.jar com.nimium.spizd.POPConnector pop3 $1 $2
