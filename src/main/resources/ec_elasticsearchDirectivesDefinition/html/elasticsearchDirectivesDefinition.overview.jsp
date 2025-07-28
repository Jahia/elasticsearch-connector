<style>
    dt {
        float: left;
        clear: left;
        width: 300px;
        text-align: right;
        font-weight: bold;
        font-size: larger;
        color: black;
    }

    dt::after {
        content: ":";
    }

    dd {
        margin: 0 0 0 310px;
        padding: 0 0 0.5em 0;
    }
</style>
<md-card class="db-card">
    <md-card-content>
        <div layout="row" layout-padding layout-wrap>
            <div flex="100" layout-padding>
                <h2 class="md-title bg-info center-align">{{egsc.title}}</h2>
            </div>
            <div flex="50">
                <dl>
                    <dt message-key="ec_elasticsearchConnector.label.statistics.hostname"></dt>
                    <dd>{{egsc.connectionStatus.host}}</dd>
                    <dt message-key="ec_elasticsearchConnector.label.statistics.port"></dt>
                    <dd>{{egsc.connectionStatus.port}}</dd>
                    <dt message-key="ec_elasticsearchConnector.label.statistics.clusterStatus"></dt>
                    <dd style="background-color: {{egsc.connectionStatus.status.status}}; width: 100px">&nbsp;</dd>
                </dl>
            </div>
            <div flex="50">
                <dl>
                    <dt message-key="ec_elasticsearchConnector.label.statistics.nodesNumber"></dt>
                    <dd>{{egsc.connectionStatus.status.numberOfNodes}}</dd>
                    <dt message-key="ec_elasticsearchConnector.label.statistics.numberOfPendingTasks"></dt>
                    <dd>{{egsc.connectionStatus.status.numberOfPendingTasks}}</dd>
                    <dt message-key="ec_elasticsearchConnector.label.statistics.activeShards"></dt>
                    <dd>{{egsc.connectionStatus.status.activeShards}}</dd>
                    <dt message-key="ec_elasticsearchConnector.label.statistics.unassignedShards"></dt>
                    <dd>{{egsc.connectionStatus.status.unassignedShards}}</dd>
                </dl>
            </div>
        </div>
    </md-card-content>
</md-card>
