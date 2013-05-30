'use strict';

angular.module('uiApp')
    .factory('socketService', function ($rootScope, $q) {
        // Keep all pending requests here until they get responses
        var callbacks = {};

        // Create a unique callback ID to map requests to responses
        var currentCallbackId = 0;

        // Create our websocket object with the address to the websocket
        var ws = new WebSocket("ws://localhost:9000/db");

        ws.onopen = function () {
            console.log("Socket has been opened!");
        };

        ws.onmessage = function (message) {
            listener(message);
        };

        function sendRequest(request) {
            var defer = $q.defer();

            if (!(request.payload.hasOwnProperty("reqId"))) {
                request["payload"].reqId = "other" + getCallbackId();
            }

            var reqId = request["payload"].reqId;

            callbacks[reqId] = {
                time: new Date(),
                cb: defer
            };

            console.log('Sending request', request);
            ws.send(JSON.stringify(request));
            return defer.promise;
        }

        function listener(data) {
            var messageObj = JSON.parse(data.data);
            console.log("Received data from websocket: ", messageObj);
            // If an object exists with reqId in our callbacks object, resolve it
            if (callbacks.hasOwnProperty(messageObj.payload.reqId)) {
                var reqId = messageObj.payload.reqId;
                console.log(callbacks[reqId]);
                $rootScope.$apply(callbacks[reqId].cb.resolve(messageObj));
                delete callbacks[reqId];
            }
        }

        // This creates a new callback ID for a request
        function getCallbackId() {
            currentCallbackId += 1;
            if (currentCallbackId > 10000) {
                currentCallbackId = 0;
            }
            return "" + currentCallbackId;
        }

        // Public API here
        return {
            sendMessage: sendRequest,
            getRequestId: getCallbackId
        };
    });
