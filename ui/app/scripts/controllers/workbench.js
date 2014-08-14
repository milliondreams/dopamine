/*global $,angular,WebSocket*/

angular.module('uiApp')
    .controller('WorkbenchCtrl', ['$rootScope', '$scope', '$location', '$timeout', 'WebSocketService', function ($rootScope, $scope, $location, $timeout, WebSocket) {
        'use strict';

        var queryId = 1;

        $scope.tabs = [
            { id: 1, title: "Query 1", query: "", result: [], active: true},
            { id: 2, title: "Query 2", query: "", result: [] }
        ];

        $scope.queryTab = [];

        function updateKeyspaces(data) {
            var keyspaces = [];
            _.each(data, function (keyspace) {
                if (keyspace.keyspace_name !== "system" && keyspace.keyspace_name !== "system_traces") {
                    keyspaces.push(keyspace);
                }
            });
            return keyspaces;
        }

        function queryResultHandler(data) {
            var expectedKeys = ["keyspace_name", "durable_writes", "strategy_class", "strategy_options"],
                dataKeys = [],
                tabId,
                resultTab;
            if (data.status === "QueryResult") {
                dataKeys = Object.keys(data.payload[0]);
                if (_.difference(dataKeys, expectedKeys).length === 0) {
                    $scope.keyspaces = updateKeyspaces(data.payload);
                } else {
                    tabId = $scope.queryTab.filter(function (t) {
                        return t.queryId === data.queryId;
                    })[0].id;
                    resultTab = _.where($scope.tabs, {id: tabId})[0];
                    if (resultTab) {
                        resultTab.error = "";
                        resultTab.result = data.payload;
                        resultTab.resultCols = _.map(Object.keys(data.payload[0]), function (k) {
                            return {field: k, displayName: k}
                        });
                    }
                }
            } else if (data.status === "Invalid Query") {
                tabId = $scope.queryTab.filter(function (t) {
                    return t.queryId === data.queryId;
                })[0].id;
                resultTab = _.where($scope.tabs, {id: tabId})[0];
                if (resultTab) {
                    resultTab.error = data.errorMsg;
                    resultTab.result = [];
                }
            }
            $scope.$apply();
        }

        $scope.submitQuery = function (id, query) {
            $scope.queryTab.push({id: id, queryId: queryId});
            WebSocket.query(query, queryId);
            queryId += 1;
        };

        function disconnectHandler(data) {
            if (data.status === "Disconnected") {
                $location.path("/");
                $scope.$apply();
            }
        }

        if ($rootScope.status === "Connected") {
            WebSocket.query("select * from system.schema_keyspaces", 0);
        }

        $scope.disconnect = function () {
            WebSocket.disconnect();
        };

        WebSocket.registerHandle(queryResultHandler);
        WebSocket.registerHandle(disconnectHandler);
    }]);
