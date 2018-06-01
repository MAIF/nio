import React, {Component} from 'react';
import PropTypes from "prop-types";
import {LabelInput, TextInput} from "../../common/ui/inputs";
import * as organisationService from "../services/OrganisationService";
import {ConsentsPage} from "./Consents";

export class OrganisationPage extends Component {

  state = {
    organisation: {
      key: '',
      version: {
        status: '',
        num: ''
      },
      label: '',
      groups: []
    },
    loading: true,
    visualizeConsents: false,
    haveReleases: true
  };

  componentDidMount() {
    this.fetch(this.props);
  };

  componentWillReceiveProps(nextProps) {
    this.fetch(nextProps);
  }

  fetch = (nextProps) => {
    if (nextProps.organisationKey) {
      organisationService.getOrganisationReleasesHistoric(nextProps.tenant, nextProps.organisationKey)
        .then(releases => this.setState({haveReleases: releases.length}))
    }

    if (nextProps.organisationKey && !nextProps.version) {
      this.setState({loading: true}, () => {
          organisationService.getOrganisationDraft(nextProps.tenant, nextProps.organisationKey)
            .then(organisation => {
              this.setState({organisation, loading: false});
            })
        }
      );
    } else if (nextProps.organisationKey && nextProps.version) {
      this.setState({loading: true}, () => {
          organisationService.getOrganisationReleaseVersion(nextProps.tenant, nextProps.organisationKey, nextProps.version)
            .then(organisation => {
              this.setState({organisation, loading: false});
            })
        }
      );
    } else {
      this.setState({loading: true}, () => {
        const organisation = {
          key: '',
          version: {
            status: '',
            num: ''
          },
          label: '',
          groups: [{
            key: '',
            label: '',
            permissions:
              [
                {
                  key: '',
                  label: ''
                }
              ]
          }]
        };

        this.setState({organisation, loading: false});
      })
    }
  };

  addGroup = () => {
    if (!this.props.readOnlyMode) {

      const group = {
        key: '',
        label: '',
        permissions:
          [
            {
              key: '',
              label: ''
            }
          ]
      };

      const groups = [...this.state.organisation.groups];
      groups.push(group);

      this.setState({organisation: {...this.state.organisation, groups}});
    }
  };

  removeGroup = (index) => {
    if (!this.props.readOnlyMode) {
      const groups = [...this.state.organisation.groups];
      groups.splice(index, 1);
      this.setState({organisation: {...this.state.organisation, groups}});
    }
  };

  onChangeGroup = (index, group) => {
    if (!this.props.readOnlyMode) {
      const groups = [...this.state.organisation.groups];
      groups[index] = group;
      this.setState({organisation: {...this.state.organisation, groups}}, () => {
        if (this.state.errors && this.state.errors.length)
          this.validate(this.state);
      });
    }
  };

  onChange = (value, name) => {
    if (!this.props.readOnlyMode)
      this.setState({organisation: {...this.state.organisation, [name]: value}}, () => {
        if (this.state.errors && this.state.errors.length)
          this.validate(this.state);
      });
  };

  validate = (nextState) => {
    const errors = [];

    if (!nextState.organisation.key) {
      errors.push("organisation.key.required");
    } else if (!/^\w+$/.test(nextState.organisation.key)) {
      errors.push("organisation.key.invalid");
    }

    if (!nextState.organisation.label)
      errors.push("organisation.label.required");

    nextState.organisation.groups.forEach((group, indexGroup) => {
      if (!group.key)
        errors.push(`organisation.groups.${indexGroup}.key.required`);
      else if (!/^\w+$/.test(group.key)) {
        errors.push(`organisation.groups.${indexGroup}.key.invalid`);
      }

      if (!group.label)
        errors.push(`organisation.groups.${indexGroup}.label.required`);

      group.permissions.forEach((permission, indexPermission) => {
        if (!permission.key)
          errors.push(`organisation.groups.${indexGroup}.permissions.${indexPermission}.key.required`);
        else if (!/^\w+$/.test(permission.key)) {
          errors.push(`organisation.groups.${indexGroup}.permissions.${indexPermission}.key.invalid`);
        }

        if (!permission.label)
          errors.push(`organisation.groups.${indexGroup}.permissions.${indexPermission}.label.required`);
      })
    });

    this.setState({errors});
    console.log("errors ", errors);
    return errors.length === 0;
  };

  save = () => {
    if (this.validate(this.state) && !this.props.readOnlyMode) {
      const organisation = {...this.state.organisation};

      if (organisation.version)
        delete organisation.version;

      if (this.props.organisationKey)
        organisationService.saveOrganisationDraft(this.props.tenant, this.props.organisationKey, organisation)
          .then(() => {
            if (this.props.onSave)
              this.props.onSave();

            if (this.props.reloadAfterSave)
              this.fetch(this.props);
          });
      else
        organisationService.createOrganisation(this.props.tenant, organisation)
          .then(() => {
            if (this.props.onSave)
              this.props.onSave();

            if (this.props.reloadAfterSave)
              this.fetch(this.props);
          });
    }
  };

  release = () => {
    if (this.validate(this.state) && !this.props.readOnlyMode) {
      const organisation = {...this.state.organisation};

      if (this.props.organisationKey) {
        organisationService.createOrganisationRelease(this.props.tenant, this.props.organisationKey)
          .then(() => {
            if (this.props.onSave)
              this.props.onSave();

            if (this.props.reloadAfterSave)
              this.fetch(this.props);
          });
      }
      else {
        if (organisation.version)
          delete organisation.version;

        organisationService.createOrganisation(this.props.tenant, organisation)
          .then(organisationCreated => {
            return organisationService.createOrganisationRelease(this.props.tenant, this.state.organisation.key)
              .then(() => {
                if (this.props.onSave)
                  this.props.onSave();

                if (this.props.reloadAfterSave)
                  this.fetch(this.props);
              });
          });
      }
    }
  };

  cancel = () => {
    if (!this.props.readOnlyMode)
      this.fetch();
  };

  toggleVisualize = () => {
    this.setState({visualizeConsents: !this.state.visualizeConsents});
  };

  render() {
    if (this.state.loading) {
      return "Loading";
    }

    const actionButtons = (
      <div className="form-buttons pull-right">
        <button className="btn btn-primary" onClick={this.toggleVisualize}>
          {
            this.state.visualizeConsents ?
              <i className="glyphicon glyphicon-eye-close"/>
              :
              <i className="glyphicon glyphicon-eye-open"/>
          }
        </button>

        <button className="btn btn-primary" onClick={this.save}>
          {
            this.props.organisationKey ?
              <i className="glyphicon glyphicon-hdd"/>
              :
              <i className="fa fa-floppy-o"/>
          }
        </button>

        <button className="btn btn-primary" onClick={this.release}>
          Définir comme version courante
        </button>

        <button className="btn btn-danger" onClick={this.cancel}><i className="glyphicon glyphicon-remove"/></button>
      </div>

    );

    return (
      <div className="row">
        {
          !this.props.organisationKey &&
          <div className="col-md-12">
            <h3>Nouvelle organisation</h3>
          </div>
        }

        {
          !this.state.haveReleases &&
          <div className="col-md-12">
            <div className="alert alert-warning">
              <h4 className="alert-heading">Attention</h4>
              Aucune version n'a été publiée pour cette organisation.
            </div>
          </div>
        }

        <div className="col-md-12">
          {
            this.props.readOnlyMode ?
              <LabelInput label={"Version"} value={this.state.organisation.version.num || "1"}/>
              :
              <LabelInput label={"Future version"} value={this.state.organisation.version.num || "1"}/>
          }

          <TextInput
            label={"Clé de l'organisation"}
            value={this.state.organisation.key}
            onChange={(e) => this.onChange(e, "key")}
            disabled={this.props.readOnlyMode ? true : !!this.props.organisationKey}
            errorKey={["organisation.key.required", "organisation.key.invalid"]}
            errorMessage={this.state.errors}
          />
          <TextInput
            label={"Libellé de l'organisation"}
            value={this.state.organisation.label}
            onChange={(e) => this.onChange(e, "label")}
            disabled={this.props.readOnlyMode}
            errorKey="organisation.label.required"
            errorMessage={this.state.errors}
          />
        </div>

        <div className="col-md-12">
          <div className="row">
            <div className="col-md-12" style={{'marginTop': '20px'}}>
              {
                !this.props.readOnlyMode &&
                <div className="btn btn-xs btn-primary pull-right" onClick={this.addGroup}>Créer un groupe
                </div>
              }
            </div>
          </div>
          {
            this.state.organisation.groups.map((group, index) =>
              <Group key={index} index={index} group={group} onChange={this.onChangeGroup}
                     onRemove={() => this.removeGroup(index)} readOnlyMode={this.props.readOnlyMode}
                     prefixe={"organisation."} errors={this.state.errors}/>
            )
          }
        </div>

        {!this.props.readOnlyMode && actionButtons}

        <div className="col-md-12">
          {
            this.state.errors && this.state.errors.length ?
              <div className="alert alert-danger">
                Veuillez vérifier votre saisie.
              </div> : ""
          }
        </div>

        {
          this.state.visualizeConsents &&
          <div className="col-md-12">
            <h3>Pré-visualisation</h3>
          </div>
        }

        {
          this.state.visualizeConsents &&
          <ConsentsPage groups={
            [
              ...this.state.organisation.groups.map(group => {
                return {
                  label: group.label,
                  consents: group.permissions.map(permission => {
                    return {
                      key: permission.key,
                      label: permission.label,
                      checked: false
                    }
                  })
                }
              })
            ]
          }/>
        }
      </div>
    );
  }
}

OrganisationPage.propTypes = {
  tenant: PropTypes.string.isRequired,
  organisationKey: PropTypes.string,
  onSave: PropTypes.func,
  reloadAfterSave: PropTypes.bool,
  readOnlyMode: PropTypes.bool,
  version: PropTypes.any
};

class Group extends Component {

  state = {
    group: {
      key: '',
      label: '',
      permissions: []
    }
  };

  componentDidMount() {
    this.setState({group: this.props.group});
  }

  componentWillReceiveProps(nextProps) {
    this.setState({group: nextProps.group});
  }

  addPermission = () => {
    if (!this.props.readOnlyMode) {
      const permission = {
        key: '',
        label: ''
      };

      const permissions = [...this.state.group.permissions];
      permissions.push(permission);

      this.setState({group: {...this.state.group, permissions}}, () => {
        this.props.onChange(this.props.index, this.state.group);
      });
    }
  };

  removePermission = (index) => {
    if (!this.props.readOnlyMode) {
      const permissions = [...this.state.group.permissions];
      permissions.splice(index, 1);
      this.setState({group: {...this.state.group, permissions}}, () => {
        this.props.onChange(this.props.index, this.state.group);
      });
    }
  };

  onChange = (value, name) => {
    if (!this.props.readOnlyMode)
      this.setState({group: {...this.state.group, [name]: value}}, () => {
        this.props.onChange(this.props.index, this.state.group);
      });
  };

  onChangePermission = (index, permission) => {
    if (!this.props.readOnlyMode) {
      const permissions = [...this.state.group.permissions];
      permissions[index] = permission;
      this.setState({group: {...this.state.group, permissions}}, () => {
        this.props.onChange(this.props.index, this.state.group);
      });
    }
  };

  render() {
    return (

      <div className="row">
        <hr/>
        <div className="form-group">
          <label className="col-sm-2 control-label"/>
          <div className="col-sm-10">
            <span className="groupe">Groupe</span>
            {
              !this.props.readOnlyMode &&
              <button type="button" className="btn btn-danger pull-right btn-xs" onClick={this.props.onRemove}>
                <i className="glyphicon glyphicon-trash"/>
              </button>
            }
          </div>
        </div>

        <TextInput label={"Clé du groupe"} value={this.state.group.key}
                   onChange={(e) => this.onChange(e, "key")}
                   disabled={this.props.readOnlyMode}
                   errorMessage={this.props.errors}
                   errorKey={[`${this.props.prefixe}groups.${this.props.index}.key.required`, `${this.props.prefixe}groups.${this.props.index}.key.invalid`]}/>
        <TextInput label={"Libellé du groupe"} value={this.state.group.label}
                   onChange={(e) => this.onChange(e, "label")}
                   disabled={this.props.readOnlyMode}
                   errorMessage={this.props.errors}
                   errorKey={`${this.props.prefixe}groups.${this.props.index}.label.required`}/>

        <div className="form-group">
          <label className="col-sm-2 control-label"/>
          <div className="col-sm-10" style={{'marginTop': '40px'}}>
            <span className="groupe">Permissions</span>
            {
              !this.props.readOnlyMode &&
              <button type="button" className="btn btn-primary btn-xs" style={{'marginLeft': '10px'}}
                      onClick={this.addPermission}>
                <i className="glyphicon glyphicon-plus"/>
              </button>
            }
          </div>
        </div>


        {
          this.state.group.permissions.map((permission, index) =>
            <Permission key={index} index={index} permission={permission} onChange={this.onChangePermission}
                        onRemove={() => this.removePermission(index)} readOnlyMode={this.props.readOnlyMode}
                        errors={this.props.errors} prefixe={`${this.props.prefixe}groups.${this.props.index}.`}/>
          )
        }
      </div>
    );
  }
}

Group.propTypes = {
  group: PropTypes.object.isRequired,
  index: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
  readOnlyMode: PropTypes.bool,
  prefixe: PropTypes.string,
  errors: PropTypes.array
};


class Permission extends Component {

  state = {
    permission: {
      key: '',
      label: ''
    }
  };

  componentDidMount() {
    this.setState({permission: this.props.permission});
  }

  componentWillReceiveProps(nextProps) {
    this.setState({permission: nextProps.permission});
  }

  onChange = (value, name) => {
    if (!this.props.readOnlyMode)
      this.setState({permission: {...this.state.permission, [name]: value}}, () => {
        this.props.onChange(this.props.index, this.state.permission);
      });
  };

  render() {
    return (
      <div>
        <div className="form-group">
          <label className="col-sm-2 control-label"/>
          <div className="col-sm-10" style={{'marginTop': '10px'}}>
            {
              !this.props.readOnlyMode &&
              <button className="btn btn-danger pull-right btn-xs" onClick={this.props.onRemove}>
                <i className="glyphicon glyphicon-trash"/>
              </button>
            }
          </div>
        </div>
        <TextInput label={"Clé de la permission"} value={this.state.permission.key}
                   onChange={(e) => this.onChange(e, "key")}
                   disabled={this.props.readOnlyMode}
                   errorMessage={this.props.errors}
                   errorKey={[`${this.props.prefixe}permissions.${this.props.index}.key.required`, `${this.props.prefixe}permissions.${this.props.index}.key.invalid`]}/>

        <TextInput label={"Libellé de la permission"} value={this.state.permission.label}
                   onChange={(e) => this.onChange(e, "label")}
                   disabled={this.props.readOnlyMode}
                   errorMessage={this.props.errors}
                   errorKey={`${this.props.prefixe}permissions.${this.props.index}.label.required`}/>

      </div>
    );
  }
}

Permission.propTypes = {
  permission: PropTypes.object.isRequired,
  index: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onRemove: PropTypes.func.isRequired,
  readOnlyMode: PropTypes.bool,
  prefixe: PropTypes.string,
  errors: PropTypes.array
};
