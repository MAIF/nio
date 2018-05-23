import React, {Component} from 'react';
import PropTypes from "prop-types";
import {Toggle} from "../../common/ui/components/Toggle";
import * as consentService from "../services/ConsentService";

export class ConsentsPage extends Component {

  state = {
    groups: [
      {
        label: 'J\'accepte de recevoir par téléphone, mail et SMS des offres personnalisées du groupe MAIF',
        consents: [
          {
            key: 'tel',
            label: 'Par contact téléphonique',
            checked: true
          },
          {
            key: 'electronique',
            label: 'Par contact électronique',
            checked: false
          },
          {
            key: 'sms-mms-vms',
            label: 'Par SMS / MMS / VMS',
            checked: true
          }
        ]
      },
      {
        label: 'J\'accepte de recevoir par téléphone, mail et SMS des offres personnalisées des partenaires du groupe MAIF',
        consents: [
          {
            key: 'tel',
            label: 'Par contact téléphonique',
            checked: true
          },
          {
            key: 'electronique',
            label: 'Par contact électronique',
            checked: true
          },
          {
            key: 'sms-mms-vms',
            label: 'Par SMS / MMS / VMS',
            checked: true
          }
        ]
      }
    ]
  };

  componentDidMount() {
    if (this.props.groups)
      this.setState({groups: this.props.groups})
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.groups)
      this.setState({groups: nextProps.groups})
  }

  onChangeGroup = (index, group) => {
    if (!this.props.readOnlyMode) {
      const groups = [...this.state.groups];
      groups[index] = group;
      this.setState({groups});
    }
  };

  saveConsents = () => {
    if (!this.props.readOnlyMode) {
      const user = {...this.props.user};
      user.groups = [...this.state.groups];

      user.userId = this.props.userId;
      user.doneBy.userId = this.props.userId;

      consentService.saveConsents(this.props.tenant, this.props.organisationKey, this.props.userId, user)
        .then(() => console.log("saved"));
    }
  };

  render() {
    return (
        <div className="col-md-12" style={{marginTop: "20px"}}>

          {
            this.state.groups.map((group, index) =>
              <Consents key={index} index={index} group={group} onChange={this.onChangeGroup}
                        readOnlyMode={this.props.readOnlyMode}/>
            )
          }

          {
            (this.props.submitable && !this.props.readOnlyMode) &&
            <div className="form-buttons pull-right">
              <button className="btn btn-primary" onClick={this.saveConsents}>
                Enregistrer
              </button>
            </div>
          }
      </div>
    );
  }
}

ConsentsPage.propTypes = {
  groups: PropTypes.array,
  submitable: PropTypes.bool,
  readOnlyMode: PropTypes.bool,
  tenant: PropTypes.string,
  organisationKey: PropTypes.string,
  userId: PropTypes.string,
  user: PropTypes.object
};


class Consents extends Component {

  state = {
    toggleValue: false,
    group: {
      label: '',
      consents: []
    }
  };

  onChangeConsents = (key, newValue) => {
    if (!this.props.readOnlyMode) {
      const consents = [...this.state.group.consents].map(c => {
        if (c.key === key) {
          c.checked = newValue;
        }
        return c;
      });

      this.setState({group: {...this.state.group, consents}}, () => {
        this.props.onChange(this.props.index, this.state.group);
      });
    }
  };

  onChangeToggleGroup = (toggleValue) => {
    if (!this.props.readOnlyMode) {
      this.setState({toggleValue}, () => {
        const consents = [...this.state.group.consents].map(c => {
          c.checked = this.state.toggleValue;
          return c;
        });

        this.setState({group: {...this.state.group, consents}}, () => {
          this.props.onChange(this.props.index, this.state.group);
        });
      });
    }
  };

  initializeToggleValue = (group) => {
    let toggleValue = true;
    group.consents.forEach(consent => {
      if (!consent.checked)
        toggleValue = false;
    });

    return toggleValue;
  };

  initializeConsents = (group) => {
    const toggleValue = this.initializeToggleValue(group);
    this.setState({group, toggleValue});
  };

  componentDidMount() {
    this.initializeConsents(this.props.group);
  }

  componentWillReceiveProps(nextProps) {
    this.initializeConsents(nextProps.group);
  }

  render() {
    return (
      <div className="row thumbnail">
        <div className="col-md-12 caption">
          <div className="row">
            <div className="col-md-12">
              {this.state.group.label}
            </div>

        <div className="col-md-12"  style={{paddingBottom: "5px", paddingTop: "5px"}}>

          {
            this.state.group.consents.map((consent, index) =>
              <div className="row" key={index}>
                <div className="col-md-12">
                  <input type="checkbox" id={`${this.props.index}-${index}-${consent.key}`}
                         checked={consent.checked}
                         onChange={() => this.onChangeConsents(consent.key, !consent.checked)}/>
                  <label style={{marginLeft: "20px"}}
                         htmlFor={`${this.props.index}-${index}-${consent.key}`}>{consent.label}</label>
                </div>
              </div>
            )
          }
        </div>
{
  !this.props.readOnlyMode &&

        <div className="col-md-2" style={{paddingBottom: "5px", paddingTop: "5px"}}>
          <div className="row">
            <div className="col-md-12" onClick={() => this.onChangeToggleGroup(!this.state.toggleValue)}>
              {
                  this.state.toggleValue ?

                      <div className="btn btn-primary btn-primary-reverse">
                          <i className="fa fa-square-o"/> Tout déselectionner
                      </div>
                      :
                      <div className="btn btn-primary btn-primary-reverse">
                          <i className="fa fa-check-square-o"/> Tout sélectionner
                      </div>
              }
            </div>
          </div>
        </div>
}
  </div>
</div>
</div>
    );
  }
}

Consents.propTypes = {
  index: PropTypes.number.isRequired,
  group: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  readOnlyMode: PropTypes.bool,
};
