'use strict';

angular.module('uiApp')
    .controller('CasuiCtrl', function ($scope, socketService, $rootScope, $location) {
        if (!($rootScope.connected)) {
            $location.path("/");
            return;
        }

        var runQuery = function (query, prefix) {
            var reqId = prefix + socketService.getRequestId();
            return socketService.sendMessage({
                "command": "query",
                "payload": {
                    "cql": query,
                    "reqId": reqId
                }
            });
        };

        $scope.keyspaces = {};

        runQuery("SELECT * from system.schema_keyspaces;", "keyspace").then(
            function (message) {
                console.log(message);
                $scope.keyspaces = message;
            }
        );

    })
;
