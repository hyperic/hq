#!/bin/sh

USER=hqadmin
HOST=localhost
PORT=7080
curl -u$USER http://$HOST:$PORT/hqu/nolert/alert/status.hqu
