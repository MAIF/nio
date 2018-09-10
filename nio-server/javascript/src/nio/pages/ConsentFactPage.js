import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as consentService from "../services/ConsentService";
import * as userExtractService from "../services/UserExtractService";
import {ConsentFactDisplayPage} from "./ConsentFactDisplayPage";
import {ConsentFactHistoryPage} from "./ConsentFactHistoryPage";
import moment from "moment";

export class ConsentFactPage extends Component {

    state = {
        selectedTab: 'CURRENT',
        tabs: []
    };

    componentDidMount() {
        consentService.getConsentsHistory(this.props.tenant, this.props.organisationKey, this.props.userId, 0, 1)
            .then(currentConsentVersion => {
                const tabs = [];

                tabs.push({
                    title: "Version courante",
                    id: 'CURRENT',
                    content: <ConsentFactDisplayPage {...this.props} user={currentConsentVersion.items[0]}
                                                     customLabel={"Version courante de l'utilisateur"}/>,
                    deletable: false
                });
                tabs.push({
                    title: "Historique",
                    id: 'HISTORY',
                    content: <ConsentFactHistoryPage {...this.props} showDetails={this.addTab}/>,
                    deletable: false
                });

                this.setState({tabs});
            });
    }

    extractData = () => {
        userExtractService.extractData(this.props.tenant, this.props.organisationKey, this.props.userId)
            .then(() => console.log("ok"));
    };

    addTab = (user) => {
        const tabs = [...this.state.tabs];

        let selectedTab = `VERSION-${user.lastUpdate}`;

        if (tabs.findIndex(tab => tab.id === selectedTab) === -1) {
            let versionDate = user.lastUpdate ? moment(user.lastUpdate).format("DD/MM/YYYY HH:mm:ss") : `NC`;
            tabs.push({
                title: `Version du ${versionDate}`,
                id: selectedTab,
                content: <ConsentFactDisplayPage {...this.props} user={user}
                                                 customLabel={`Aperçu de la version du ${versionDate}`}/>,
                deletable: true
            });
        }

        this.setState({tabs, selectedTab});
    };

    removeTab = (index) => {
        this.setState({selectedTab: 'CURRENT'}, () => {
            const tabs = [...this.state.tabs];
            tabs.splice(index, 1);
            this.setState({tabs});
        });
    };

    render() {
        return (
            <div className="row">
                <div className="col-md-12">
                    <h1>Utilisateur {this.props.userId}</h1>

                    <div className="form-buttons pull-right">
                        <button className="btn btn-primary" title="extract-data" onClick={this.extractData}><i className="glyphicon glyphicon-download-alt" /></button>
                    </div>
                </div>
                <div className="col-md-12">
                    <ul className="nav nav-tabs">
                        {
                            this.state.tabs.map((tab, index) =>
                                tab.deletable ?
                                    <li key={tab.id}
                                        className={`nav-item ${tab.id === this.state.selectedTab ? "active" : ""}`}>
                                        <a className="nav-link">
                                            <span
                                                onClick={() => this.setState({selectedTab: tab.id})}>{tab.title}</span> {tab.deletable &&
                                        <i className="glyphicon glyphicon-remove"
                                           onClick={() => this.removeTab(index)}/>}
                                        </a>
                                    </li>
                                    :
                                    <li key={tab.id}
                                        className={`nav-item ${tab.id === this.state.selectedTab ? "active" : ""}`}
                                        onClick={() => this.setState({selectedTab: tab.id})}>
                                        <a className="nav-link">
                                            {tab.title}
                                        </a>
                                    </li>
                            )
                        }
                    </ul>
                </div>

                <div className="">
                    <div className="col-md-12">

                        <div className="tab-content" style={{marginTop: "20px"}}>
                            {
                                this.state.tabs && this.state.tabs.length &&
                                this.state.tabs.find(tab => tab.id === this.state.selectedTab).content
                            }
                        </div>
                    </div>
                </div>

            </div>
        );
    }
}

ConsentFactPage.propTypes = {
    tenant: PropTypes.string.isRequired,
    organisationKey: PropTypes.string.isRequired,
    userId: PropTypes.string.isRequired
};