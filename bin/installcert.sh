DIR=`dirname $0`
java -cp $DIR/../lib/mail-1.4.jar:$DIR/../dist/spizd.jar com.nimium.spizd.InstallCert $1 $2
