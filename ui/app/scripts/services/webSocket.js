/*global $,angular,WebSocket*/

angular.module('uiApp')
    .factory('WebSocketService', function () {
        'use strict';
        var wsUri = "ws://localhost:9000/ws",
            websocket = new WebSocket(wsUri),
            resultHandlers = [];

        function send(msg) {
            websocket.send(JSON.stringify(msg));
        }

        function connect(msg) {
            msg.messageType = "connect";
            send(msg);
        }

        function query(queryString, queryId) {
            var updatedQuery = queryString.trim(),
                message = {"messageType": "query", "queryId": queryId};
            if (updatedQuery[updatedQuery.length - 1] !== ";") {
                updatedQuery += ";";
            }
            message.query = updatedQuery;
            send(message);
        }

        websocket.onmessage = function (evt) {
            var eventData = JSON.parse(evt.data);
            resultHandlers.forEach(function (handler) {
                handler.call(this, eventData);
            });
        };

        return {
            connect: connect,
            query: query,
            registerHandle: function (handler) {
                resultHandlers.push(handler);
            }
        };
    });
