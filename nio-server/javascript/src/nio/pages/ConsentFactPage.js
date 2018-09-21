import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as consentService from "../services/ConsentService";
import * as userExtractService from "../services/UserExtractService";
import {ConsentFactDisplayPage} from "./ConsentFactDisplayPage";
import {ConsentFactHistoryPage} from "./ConsentFactHistoryPage";
import moment from "moment";
import {UserExtractPage} from "./UserExtractPage";

export class ConsentFactPage extends Component {

    state = {
        selectedTab: 'CURRENT',
        tabs: [],
        email: "",
        errors: []
    };

    handleInputChange = (event) => {
        const target = event.target;
        const value = target.type === 'checkbox' ? target.checked : target.value;
        const name = target.name;

        const nextState = Object.assign({}, this.state, {[name]: value});
        this.setState(nextState);
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

                tabs.push({
                    title: "Historique extractions",
                    id: "EXTRACTPAGE",
                    content: <UserExtractPage tenant={this.props.tenant} organisationKey={this.props.organisationKey} userId={this.props.userId}/>,
                    deletable: false
                });

                this.setState({tabs});
            });
    }

    extractData = () => {
        if (this.validateExtract(this.state))
            userExtractService.extractData(this.props.tenant, this.props.organisationKey, this.props.userId, {email: this.state.email})
                .then(() => $('#emailAddress').modal('toggle'))
                .catch(() => $('#emailAddress').modal('toggle'));
    };

    validateExtract = (state) => {
        const errors = [];
        if (!state.email) {
            errors.push("email.error")
        } else {
            const re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

            if (!re.test(state.email)) {
                errors.push("email.format.error")
            }
        }

        this.setState({errors});
        console.log(errors);

        return !errors.length;
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
                        <button className="btn btn-primary" title="extract-data" data-toggle="modal"
                                data-target="#emailAddress"><i
                            className="glyphicon glyphicon-download-alt"/></button>
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

                <div className="modal fade" tabIndex="-1" role="dialog" id="emailAddress">
                    <div className="modal-dialog" role="document">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h4 className="modal-title text-center">Merci de saisir votre adresse email</h4>
                            </div>
                            <div className="modal-body text-center">
                                <div
                                    className={this.state.errors.indexOf("email.error") !== -1 || this.state.errors.indexOf("email.format.error") !== -1 ? "form-group has-error" : "form-group"}>
                                    <label htmlFor="emailAddress" className="col-xs-12 col-sm-2 control-label">E-mail
                                        :</label>
                                    <div className="col-sm-10 input-group">
                                        <input type="text" className="form-control" id="emailAddress" name="email"
                                               placeholder="mon-adresse@perso.fr" value={this.state.email}
                                               onChange={this.handleInputChange}/>
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-danger"
                                        data-dismiss="modal">Cancel
                                </button>

                                <button type="button" className="btn btn-success"
                                        onClick={this.extractData}>Extraire
                                </button>
                            </div>
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