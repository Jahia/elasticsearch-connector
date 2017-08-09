<%@page contentType="text/javascript" %>
    <%@ taglib prefix="dbc" uri="http://www.jahia.org/dbconnector/functions" %>
    <%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

    (function() {
        var connectionCPUUsage = function(contextualData, dcTemplateResolver) {
            var directive = {
                restrict    : 'E',
                templateUrl: function(el, attrs) {
                    return dcTemplateResolver.resolveTemplatePath('${dbc:addDatabaseConnectorModulePath('/database-connector-directives/elasticsearch-directives', renderContext)}', 'cpu');
                },
                controller  : ConnectionCPUUsageController,
                controllerAs : 'ccuc',
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
            .directive('elasticsearchCpu', ['contextualData', 'dcTemplateResolver', connectionCPUUsage]);

        var ConnectionCPUUsageController = function($scope, dcConnectionStatusService, i18n) {
            var ccuc            = this;
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
            ccuc.getHeight = getHeight;
            ccuc.getWidth  = getWidth;

            ccuc.$onInit = function() {
                init();
            };

            function init() {
                ccuc.connectionStatus = dcConnectionStatusService.getCurrentConnectionStatus();
                initChart();
                $scope.$on('connectionStatusUpdate', function(event, connectionStatus) {
                    ccuc.connectionStatus = JSON.parse(connectionStatus);
                    updateChartEntries(ccuc.connectionStatus);
                });
            }

            function getHeight() {
                return _.isUndefined(ccuc.chartHeight) || ccuc.chartHeight === null || _.isString(ccuc.chartHeight) && ccuc.chartHeight == '' ? DEFAULT_HEIGHT : ccuc.chartHeight;
            }

            function getWidth() {
                return _.isUndefined(ccuc.chartWidth) || ccuc.chartWidth === null || _.isString(ccuc.chartWidth) && ccuc.chartWidth == '' ? DEFAULT_WIDTH : ccuc.chartWidth;
            }

            function initChart() {
                ccuc.pointSize = _.isUndefined(ccuc.pointSize) || ccuc.pointSize === null || _.isString(ccuc.pointSize) && ccuc.pointSize == '' ? DEFAULT_POINT_SIZE : ccuc.pointSize;
                ccuc.memoryUsageChart = {
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
                                label   : i18n.message('ec_elasticsearchConnector.label.cpuUsed'),
                                type    : 'number',
                                p       : {}
                            }
                        ],
                        rows: []
                    },
                    options: {
                        title               : i18n.message('ec_elasticsearchConnector.label.cpuUsage'),
                        colors              : ['#009900', '#3366ff', '#cc66ff'],
                        fill                : 20,
                        displayExactValues  : true,
                        pointSize: ccuc.pointSize,
                        vAxis               : {
                            title: i18n.message('ec_elasticsearchConnector.label.cpuPercent'),
                            gridlines: {
                                count: 10
                            }
                        }
                    }
                };
            }

            function updateChartEntries(connectionStatus) {
                var cpu = connectionStatus.nodesStats.process.cpuPercent;
                var entry = angular.copy(CHART_ENTRY_TEMPLATE);
                entry.c[0].v = moment(connectionStatus.localTime).format('HH:mm:ss').toString();
                entry.c[1].v = cpu;
                entry.c[1].f = cpu + ' %';

                if (ccuc.memoryUsageChart.data.rows.length === 20) {
                    ccuc.memoryUsageChart.data.rows.shift();
                }
                ccuc.memoryUsageChart.data.rows.push(entry);
            }
        };

        ConnectionCPUUsageController.$inject = ['$scope', 'dcConnectionStatusService', 'i18nService'];
    })();