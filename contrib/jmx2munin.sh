#!/bin/bash
#
# Munin magic markers
#%# family=auto
#%# capabilities=autoconf
#

#set -x

# cut off the part after _ in symlink name
scriptname=${0##*/}

jmxfuncUpper=${scriptname##*_}
jmxfunc=${jmxfuncUpper,,}

query='tlc2.*:type='$jmxfuncUpper
config='tlc2/tlc'

if [ -z "$MUNIN_LIBDIR" ]; then
    MUNIN_LIBDIR="`dirname $(dirname "$0")`"
fi

if [ -f "$MUNIN_LIBDIR/plugins/plugin.sh" ]; then
    . $MUNIN_LIBDIR/plugins/plugin.sh
fi

if [ "$1" = "autoconf" ]; then
    echo yes
    exit 0
fi

if [ -z "$url" ]; then
  # this is very common so make it a default
  url="service:jmx:rmi:///jndi/rmi://127.0.0.1:5400/jmxrmi"
fi

if [ -z "$config" -o -z "$query" -o -z "$url" ]; then
  echo "Configuration needs attributes config, query and optinally url"
  exit 1
fi

JMX2MUNIN_DIR="/etc/munin/plugin-conf.d"
CONFIG="$JMX2MUNIN_DIR/$config"

if [ "$1" = "config" ]; then


case "$jmxfunc" in
    *modelchecker*)
cat <<__EOF
graph_title $jmxfuncUpper
graph_scale yes
graph_category tlc
graph_vlabel statesGenerated
graph_vlabel statesGeneratedPerMinute
graph_vlabel distinctStatesGenerated
graph_vlabel distinctStatesGeneratedPerMinute
graph_vlabel stateQueueSize
graph_vlabel progressLevel
__EOF
echo tlc2_tool_"$jmxfunc"_distinctstatesgenerated.label distinctStatesGenerated
echo tlc2_tool_"$jmxfunc"_progress.label progressLevel
echo tlc2_tool_"$jmxfunc"_distinctstatesgeneratedperminute.label distinctStatesGeneratedPerMinute
echo tlc2_tool_"$jmxfunc"_statequeuesize.label stateQueueSize
echo tlc2_tool_"$jmxfunc"_statesgenerated.label statesGenerated
echo tlc2_tool_"$jmxfunc"_statesgeneratedperminute.label statesGeneratedPerMinute
    ;;

    *diskfpset*)
cat <<__EOF
graph_title $jmxfuncUpper
graph_scale yes
graph_category tlc
graph_vlabel tblCnt
graph_vlabel fileCnt
graph_vlabel indexCnt
graph_vlabel diskLookupCnt
graph_vlabel memHitCnt
graph_vlabel diskHitCnt
graph_vlabel diskWriteCnt
graph_vlabel diskSeekCnt
graph_vlabel growDiskMark
graph_vlabel checkpointMark
__EOF
echo tlc2_tool_fp_"$jmxfunc"_filecnt.label fileCnt
echo tlc2_tool_fp_"$jmxfunc"_indexcnt.label indexCnt
echo tlc2_tool_fp_"$jmxfunc"_disklookupcnt.label diskLookupCnt
echo tlc2_tool_fp_"$jmxfunc"_memhitcnt.label memHitCnt
echo tlc2_tool_fp_"$jmxfunc"_diskhitcnt.label diskHitCnt
echo tlc2_tool_fp_"$jmxfunc"_diskwritecnt.label diskWriteCnt
echo tlc2_tool_fp_"$jmxfunc"_diskseekcnt.label diskSeekCnt
echo tlc2_tool_fp_"$jmxfunc"_tblcnt.label tblCnt
echo tlc2_tool_fp_"$jmxfunc"_growdiskmark.label growDiskMark
echo tlc2_tool_fp_"$jmxfunc"_checkpointmark.label checkpointMark
    ;;
esac
    #cat "$CONFIG"
    exit 0
fi

JAR="/usr/share/munin/jmx2munin.jar"
CACHED="/tmp/jmx2munin"$jmxfuncUpper

if test ! -f $CACHED || test `find "$CACHED" -mmin +2`; then

    java -jar "$JAR" \
      -url "$url" \
      -query "$query" \
      $ATTRIBUTES \
      > $CACHED

    echo "cached.value `date +%s`" >> $CACHED
fi

ATTRIBUTES=`awk '/\.label/ { gsub(/\.label/,""); print $1 }' $CONFIG`

if [ -z "$ATTRIBUTES" ]; then
  echo "Could not find any *.label lines in $CONFIG"
  exit 1
fi

for ATTRIBUTE in $ATTRIBUTES; do
  grep $ATTRIBUTE $CACHED
done

exit 0