import React, {Component} from 'react';
import PropTypes from "prop-types";
import _ from "lodash";
import * as consentService from "../services/ConsentService";

export class ConsentsPage extends Component {

  state = {groups: _.cloneDeep(this.props.groups) || [], offers: _.cloneDeep(this.props.offers) || []};

  onChangeGroup = (index, group) => {
    if (!this.props.readOnlyMode) {
      const groups = [...this.state.groups];
      groups[index] = group;
      this.setState({groups});
    }
  };

  componentWillReceiveProps(nextProps) {
    this.setState({groups: nextProps.groups || [], offers: nextProps.offers || []})
  }

  onChangeOffer = (index, offer) => {
    if (!this.props.readOnlyMode) {
      const offers = _.cloneDeep(this.state.offers);
      offers[index] = offer;
      this.setState({offers})
    }
  };

  saveConsents = () => {
    if (!this.props.readOnlyMode) {
      const user = {...this.props.user};
      user.groups = [...this.state.groups];
      user.offers = [...this.state.offers];

    if (user.offers && !user.offers.length)
        delete user.offers;

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
              this.state.offers && this.state.offers.map((offer, index) =>
                  <Offer key={index} offer={offer} index={index} onChange={this.onChangeOffer}
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
  offers: PropTypes.array,
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
    this.setState({group: {...group}, toggleValue});
  };

  componentDidMount() {
    this.initializeConsents(_.cloneDeep(this.props.group));
  }

  componentWillReceiveProps(nextProps) {
    this.initializeConsents(_.cloneDeep(nextProps.group));
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

class Offer extends Component {
    state = {
        toggleValue: false,
        offer: {
            key: '',
            label: '',
            groups: []
        }
    };

    componentDidMount() {
        this.setState({offer: _.cloneDeep(this.props.offer)})
    }

    componentWillReceiveProps(nextProps) {
        this.setState({offer: _.cloneDeep(nextProps.offer)})
    }

    onChangeOfferGroup = (index, group) => {
        if (!this.props.readOnlyMode) {
            const offer = _.cloneDeep(this.state.offer);
            offer.groups[index] = group;
            this.props.onChange(this.props.index, offer)
        }
    };

    render() {
        return (
            <div className="row thumbnail">
                <div className="col-md-12">
                    <div className="row">
                        <div className="col-md-12">
                            {this.state.offer.label}
                        </div>

                        <div className="col-md-12"  style={{paddingBottom: "5px", paddingTop: "5px"}}>
                            {
                                this.state.offer.groups.map((group, index) =>
                                    <Consents key={index} index={index} group={group} onChange={this.onChangeOfferGroup}
                                              readOnlyMode={this.props.readOnlyMode}/>
                                )
                            }
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}


Offer.propTypes = {
    index: PropTypes.number.isRequired,
    offer: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    readOnlyMode: PropTypes.bool,
};
