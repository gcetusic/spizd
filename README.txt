0) INTRO

SPIZD stands for Stress Probing Invasive Zap Destructor;
it's a stress test tool we use to test our servers.

While there is a number of tools to bomb your servers with enormous number of
requests (like ab distributed with apache httpd), we needed a tool to check
how much simultaneous connections our servers can handle.
Numbers of connections and requests are quite different things, especially for
different protocols.
We could not find any such general too, so we wrote it.

Brought to you by Nimium, http://www.nimium.hr/
under GPL lincese, for details check gpl.txt in this directory.


1) INSTALLING

Get the archive and unpack.


2) CONFIGURING

Edit etc/spizd.properties.


3) RUNNING

Make sure you read and customize etc/spizd.propeties file.

There's a few script files in bin directory, they all require the property file.

spizd-probe.sh will open a number of connections to given server, and read one
line of response from each of them, trying to determine number of simultaneous
connctions a server can handle.
This is probably the first thing you'll be running.

spizd-http.sh reads given url list file, and bombs the server with one request
per connection, keeping constant number of concurrent connections, or decreasing
number of connections, as configured.

spizd-pop3.sh reads password dictionary file, and tries to login to a pop server
with given username and passwords read from the dictionary, via multiple
connections.

spizd-imap.sh does the same thing for imap servers.

spizd-smtp.sh does the same thing for smtp servers. Additionally, it can send
a number of messages for each connection, see spizd.properties.

spizd-ssh.sh does the same thing for ssh.
Note: default confing of sshd contains MaxStartups "10:30:60", meaning
10 unauthenticated connections are allowed, 10-60 have 30% disconnect chance,
and all above 60 are disallowed.

spizd-pop3s.sh, spizd-pop3tls.sh, spizd-imaps.sh, spizd-imaptls.sh,
spizd-smtps.sh, spizd-smtptls.sh
use secure authentification mechanisms for mail servers.

installcert.sh is utility to install self-signed certificates if required.

spizd-radius.sh reads dictionary file and sends auth-request packets to
destigned radius server. Options like radius secret need to be specified in
spizd.properties file.

spizd-dict.sh generates password dictionary file within given parameters.
Password generation is not very smart, and generated dictionary file contains
passwords sorted alphabetically, rough equivalent of brute-force attack;
if you really need to crack some passwords, get some better dictionary file(s).

Dictionary file name is specified in properties file.
File may contain only passwords, or login and password separated by defined
separator; in former case login must be provided as command line argument.

For more information on how each of these modules work, check included javadoc,
or - Use the Source, Luke!

Note for crackers:
this is invasive cracking method and you will be noticed immediatelly.


4) BUILDING

There's ant build.xml file in this directory, so to build SPIZD distro,
just type ant dist.


5) LINUX NOTES

Running a stress-test client is not a trivial task.

A linux user (including root) can have only 1024 files and sockets opened
by default; check ulimit -n and nofile in limits.conf.

Furthermore, number of local ports is also limited; check
/proc/sys/net/ipv4/ip_local_port_range
(net.ipv4.ip_local_port_range in sysctl.conf).

Kernel IP connection tracking module is also limited; check
/proc/sys/net/ipv4/ip_conntrack_max
(net.ipv4.ip_conntrack_max in sysctl.conf)
and /proc/sys/net/nf_conntrack_max
(net.netfilter.nf_conntrack_max).

If that's not enough, you may also need to change
/proc/sys/net/ipv4/tcp_tw_reuse (net.ipv4.tcp_tw_reuse in sysctl.conf)
to 1, to reuse sockets in TIME_WAIT state, or to reduce
/proc/sys/net/ipv4/tcp_fin_timeout (net.ipv4.tcp_fin_timeout in sysctl.conf)
so sockets wait less for the other peer to close.

Also, server may detect stress test as syn flood and start sending syn cookies;
this may result in connection timeouts/refusal.
If so, set /proc/sys/net/ipv4/tcp_syncookies to 0.
