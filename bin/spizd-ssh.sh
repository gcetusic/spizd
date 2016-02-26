DIR=`dirname $0`
java -cp $DIR/../lib/ganymed-ssh2-build251beta1.jar:$DIR/../dist/spizd.jar com.nimium.spizd.SSHConnector $1 $2
