#!/usr/bin/env bash
# خدمة المعاينة — تراقب نفسها: لو السيرفر مات يرجّع يشغل
cd /home/z/my-project
while true; do
  if ! curl -s --max-time 3 -o /dev/null http://localhost:3000/; then
    pkill -f "http.server 3000" 2>/dev/null
    sleep 1
    python3 -m http.server 3000 -d dist < /dev/null >> /tmp/static-server.log 2>&1 &
  fi
  sleep 10
done
