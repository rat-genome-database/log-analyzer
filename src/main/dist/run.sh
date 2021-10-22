# analyzes FTP files
#
. /etc/profile
APPNAME=log-analyzer
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAIL_LIST=mtutaj@mcw.edu

# run java app by calling gradle-generated wrapper script
cd $APPDIR
java -Xmx200g -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/$APPNAME.jar "$@" | tee run.log 2>&1

#mailx -s "[$SERVER] LogAnalyzer OK!" $EMAIL_LIST < $APPDIR/logs/summary.log

