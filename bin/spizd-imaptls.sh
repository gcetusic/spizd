DIR=`dirname $0`
java -cp $DIR/../lib/mail-1.4.jar:$DIR/../dist/spizd.jar com.nimium.spizd.IMAPConnector imap-tls $1 $2
