import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as organisationService from "../services/OrganisationService";
import {OrganisationPage} from "./OrganisationPage";
import {OrganisationReleasePage} from "./OrganisationReleasePage";
import {OrganisationDiffPage} from "./OrganisationDiffPage";

export class OrganisationVersionPage extends Component {

  state = {
    selectedTab: 'DRAFT',
    tabs: []
  };

  componentDidMount() {
    const tabs = [];

    tabs.push({
      title: "Brouillon",
      id: "DRAFT",
      content: <OrganisationPage tenant={this.props.tenant} organisationKey={this.props.organisationKey}
                                 readOnlyMode={false} reloadAfterSave={true}/>,
      deletable: false
    });

    tabs.push({
      title: "Publications",
      id: "RELEASE",
      content: <OrganisationReleasePage tenant={this.props.tenant} organisationKey={this.props.organisationKey}
                                        showDetails={this.addTab}/>,
      deletable: false
    });

    tabs.push({
      title: "Différences",
      id: "DIFF",
      content: <OrganisationDiffPage tenant={this.props.tenant} organisationKey={this.props.organisationKey}/>,
      deletable: false
    });

    this.setState({tabs});
  }

  addTab = (version) => {
    const tabs = [...this.state.tabs];

    let selectedTab = `RELEASE-${version}`;

    if (tabs.findIndex(tab => tab.id === selectedTab) === -1)
      tabs.push({
        title: `Version ${version}`,
        id: selectedTab,
        content: <OrganisationPage tenant={this.props.tenant} organisationKey={this.props.organisationKey}
                                   readOnlyMode={true} version={version}/>,
        deletable: true
      });

    this.setState({tabs, selectedTab});
  };

  removeTab = (index) => {
    this.setState({selectedTab: 'DRAFT'}, () => {
      const tabs = [...this.state.tabs];
      tabs.splice(index, 1);
      this.setState({tabs});
    });
  };

  render() {
    return (
      <div className="row">
        <div className="col-md-12">
          <h1>Organisation {this.props.organisationKey}</h1>
        </div>
        <div className="col-md-12">
          <ul className="nav nav-tabs">
            {
              this.state.tabs.map((tab, index) =>
                tab.deletable ?
                  <li key={tab.id} className={`nav-item ${tab.id === this.state.selectedTab ? "active" : ""}`}>
                    <a className="nav-link">
                      <span onClick={() => this.setState({selectedTab: tab.id})}>{tab.title}</span> {tab.deletable &&
                    <i style={{cursor: 'pointer'}} className="glyphicon glyphicon-remove" onClick={() => this.removeTab(index)}/>}
                    </a>
                  </li>
                  :
                  <li key={tab.id} className={`nav-item ${tab.id === this.state.selectedTab ? "active" : ""}`}
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

OrganisationVersionPage.propTypes = {
  tenant: PropTypes.string.isRequired,
  organisationKey: PropTypes.string.isRequired
};
