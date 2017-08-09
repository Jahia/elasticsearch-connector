<%@page contentType="text/javascript" %>
    <%@ taglib prefix="dbc" uri="http://www.jahia.org/dbconnector/functions" %>
    <%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

    (function() {
        var connectionMemoryUsage = function(contextualData, dcTemplateResolver) {
            var directive = {
                restrict    : 'E',
                templateUrl: function(el, attrs) {
                    return dcTemplateResolver.resolveTemplatePath('${dbc:addDatabaseConnectorModulePath('/database-connector-directives/elasticsearch-directives', renderContext)}', 'memory');
                },
                controller  : ConnectionMemoryUsageController,
                controllerAs : 'cmuc',
                bindToController : {
                    chartHeight : '=',
                    chartWidth  : '=',
                    pointSize   : '=?'
                },
                link        : linkFunc
            };

            function linkFunc($scope, el, attr, ctrls) {}

            return directive;
        };

        angular
            .module('databaseConnector')
            .directive('elasticsearchMemory', ['contextualData', 'dcTemplateResolver', connectionMemoryUsage]);

        var ConnectionMemoryUsageController = function($scope, dcConnectionStatusService, i18n) {
            var cmuc            = this;
            var DEFAULT_HEIGHT  = '480px';
            var DEFAULT_WIDTH   = '640px';
            var DEFAULT_POINT_SIZE = 5;

            var CHART_ENTRY_TEMPLATE = {
                "c": [
                    {
                        v: ''
                    },
                    {
                        v: ''
                    }
                ]
            };
            cmuc.getHeight = getHeight;
            cmuc.getWidth  = getWidth;

            cmuc.$onInit = function() {
                init();
            };

            function init() {
                cmuc.connectionStatus = dcConnectionStatusService.getCurrentConnectionStatus();
                initChart();
                $scope.$on('connectionStatusUpdate', function(event, connectionStatus) {
                    cmuc.connectionStatus = JSON.parse(connectionStatus);
                    updateChartEntries(cmuc.connectionStatus);
                });
            }

            function getHeight() {
                return _.isUndefined(cmuc.chartHeight) || cmuc.chartHeight === null || _.isString(cmuc.chartHeight) && cmuc.chartHeight == '' ? DEFAULT_HEIGHT : cmuc.chartHeight;
            }

            function getWidth() {
                return _.isUndefined(cmuc.chartWidth) || cmuc.chartWidth === null || _.isString(cmuc.chartWidth) && cmuc.chartWidth == '' ? DEFAULT_WIDTH : cmuc.chartWidth;
            }

            function initChart() {
                cmuc.pointSize = _.isUndefined(cmuc.pointSize) || cmuc.pointSize === null || _.isString(cmuc.pointSize) && cmuc.pointSize == '' ? DEFAULT_POINT_SIZE : cmuc.pointSize;
                cmuc.memoryUsageChart = {
                    type        : 'LineChart',
                    displayed   : true,
                    data        : {
                        cols: [
                            {
                                id      : 'localTime',
                                label   : '',
                                type    : 'string',
                                p       : {}
                            },
                            {
                                id      : 'mapped',
                                label   : i18n.message('ec_elasticsearchMemory.label.heapUsed'),
                                type    : 'number',
                                p       : {}
                            }
                        ],
                        rows: []
                    },
                    options: {
                        title               : i18n.message('ec_elasticsearchMemory.label.heapUsage'),
                        colors              : ['#009900', '#3366ff', '#cc66ff'],
                        fill                : 20,
                        displayExactValues  : true,
                        pointSize: cmuc.pointSize,
                        hAxis               : {
                            scaleType: "log"
                        },
                        vAxis               : {
                            title: i18n.message('dc_databaseConnector.label.megabytes'),
                            gridlines: {
                                count: 10
                            }
                        }
                    }
                };
            }

            function updateChartEntries(connectionStatus) {
                var heapUsed = connectionStatus.nodesStats.jvm.heapUsed / (1024 * 1024);
                heapUsed = heapUsed.toFixed(2);
                var entry = angular.copy(CHART_ENTRY_TEMPLATE);
                <%--entry.c[0].v = moment(connectionStatus.localTime).format('HH:mm:ss').toString();--%>
                entry.c[1].v = heapUsed;
                entry.c[1].f = heapUsed + ' MB';

                if (cmuc.memoryUsageChart.data.rows.length === 100) {
                    cmuc.memoryUsageChart.data.rows.shift();
                }
                cmuc.memoryUsageChart.data.rows.push(entry);
            }
        };

        ConnectionMemoryUsageController.$inject = ['$scope', 'dcConnectionStatusService', 'i18nService'];
    })();