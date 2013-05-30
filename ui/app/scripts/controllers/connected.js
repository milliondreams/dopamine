'use strict';

angular.module('uiApp')
    .controller('ConnectedCtrl', function ($scope, socketService) {
        $scope.query = '';
        $scope.queries = {};
        $scope.resultGrid = {data: 'queries[reqId].result'};

        $scope.runQuery = function () {
            if ($scope.query.length !== 0) {
                var reqId = "query-" + socketService.getRequestId();
                console.log(reqId);
                $scope.queries[reqId] = {"cql": $scope.query, "reqId": reqId, result: [] };
                socketService.sendMessage({"command": "query", "payload": $scope.queries[reqId]}).then(
                    function (message) {
                        $scope.queries[message.payload.reqId].result = message.payload.response;
                    }
                );
            }
        };
    });
