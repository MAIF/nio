import React, {Component} from 'react';
import PropTypes from "prop-types";

const OnSwitch = props =>
  <div className="content-switch-button-on" onClick={props.onChange}>
    <div className="switch-button-on"/>
  </div>;

const OffSwitch = props =>
  <div className="content-switch-button-off" onClick={props.onChange}>
    <div className="switch-button-off"/>
  </div>;

export class Toggle extends Component {

  toggleOff = e => {
    if (e && e.preventDefault) e.preventDefault();
    this.props.onChange(false);
  };

  toggleOn = e => {
    if (e && e.preventDefault) e.preventDefault();
    this.props.onChange(true);
  };


  render() {
    const value = this.props.value;
    return (
      <div>
        {value && <OnSwitch onChange={this.toggleOff}/>}
        {!value && <OffSwitch onChange={this.toggleOn}/>}
      </div>
    );
  }
}

Toggle.propTypes = {
  value: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired
};