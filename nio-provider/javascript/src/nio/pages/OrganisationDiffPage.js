import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as organisationService from "../services/OrganisationService";
import {ReactDiff} from "../../common/ui/components/ReactDiff";
import {SelectInput} from "../../common/ui/inputs/SelectInput";

export class OrganisationDiffPage extends Component {

  errorsMessage = [
    {key: "numV1.required", label: "La première version est obligatoire."},
    {key: "numV2.required", label: "La seconde version est obligatoire."}
  ];

  state = {
    v1: null,
    v2: null,
    numV1: '',
    numV2: '',
    loading: true,
    format: 'splited',
    errors: [],
    releasesVersion: []
  };

  componentDidMount() {
    this.fetch(this.props);
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.organisationKey !== nextProps.organisationKey)
      this.fetch(nextProps);
  }

  fetch = (nextProps) => {
    this.setState({loading: true}, () => {
      organisationService.getOrganisationReleasesHistoric(nextProps.tenant, nextProps.organisationKey)
        .then(releases => this.setState({
          releasesVersion: releases.map(release => release.version.num),
          loading: false
        }));
    });
  };

  compare = () => {
    if (this.validate())
      this.setState({loading: true}, () => {
        organisationService.getOrganisationReleaseVersion(this.props.tenant, this.props.organisationKey, this.state.numV1)
          .then(v1 => {
            organisationService.getOrganisationReleaseVersion(this.props.tenant, this.props.organisationKey, this.state.numV2)
              .then(v2 => {
                this.setState({v1, v2, loading: false});
              })
          })
      });
  };

  validate = () => {
    const errors = [];

    if (!this.state.numV1)
      errors.push("organisation.diff.numV1.required");

    if (!this.state.numV2)
      errors.push("organisation.diff.numV2.required");

    this.setState({errors});
    return errors.length === 0;
  };

  onChange = (value, name) => {
    this.setState({[name]: value}, () => {
      if (this.validate())
        this.compare();
    });
  };

  render() {
    if (this.state.loading)
      return "Loading...";

    return (
      <div className="row">
        <div className="col-md-12">
          <h3>Comparer les versions publiées</h3>
        </div>

        <div className="col-md-12">
          <SelectInput label={"Première version"} value={this.state.numV1} possibleValues={this.state.releasesVersion}
                       onChange={(value) => this.onChange(value, "numV1")} errorKey={"organisation.diff.numV1.required"}
                       errorMessage={this.state.errors}/>

          <SelectInput label={"Seconde version"} value={this.state.numV2} possibleValues={this.state.releasesVersion}
                       onChange={(value) => this.onChange(value, "numV2")} errorKey={"organisation.diff.numV2.required"}
                       errorMessage={this.state.errors}/>

          <div className="col-md-12">
            {
              this.state.errors && this.state.errors.length ?
                <div className="alert alert-danger">
                  Veuillez vérifier votre saisie.
                </div> : ""
            }
          </div>

          {/*{*/}
            {/*this.state.v1 && this.state.v2 &&*/}
            {/*<div className="btn-group btn-group-toggle" data-toggle="buttons">*/}
              {/*<label className={`btn btn-secondary ${this.state.format === "unified" ? "active" : ""}`}*/}
                     {/*onClick={() => this.setState({format: "unified"})}>*/}
                {/*<input type="radio" name="options" id="option1" autoComplete="off"*/}
                       {/*checked={this.state.format === "unified"}*/}
                       {/*onChange={() => {*/}
                       {/*}}/> Unifier*/}
              {/*</label>*/}
              {/*<label className={`btn btn-secondary ${this.state.format === "splited" ? "active" : ""}`}*/}
                     {/*onClick={() => this.setState({format: "splited"})}>*/}
                {/*<input type="radio" name="options" id="option2" autoComplete="off"*/}
                       {/*checked={this.state.format === "splited"}*/}
                       {/*onChange={() => {*/}
                       {/*}}/> Séparer*/}
              {/*</label>*/}
            {/*</div>*/}
          {/*}*/}
          {
            this.state.v1 && this.state.v2 &&
            <ReactDiff inputA={this.state.v1} inputB={this.state.v2} type="json" format={this.state.format}/>
          }
        </div>
      </div>
    );
  }
}

OrganisationDiffPage.propTypes = {
  tenant: PropTypes.string.isRequired,
  organisationKey: PropTypes.string.isRequired,
};
