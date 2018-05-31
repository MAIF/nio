import React from 'react';
import PropTypes from 'prop-types'; // ES6
import * as errorManager from "../../../nio/services/ErrorManagerService";

export const FieldError = (props) => {
  const compare = (message) => {
    if (typeof props.errorKey === 'string') {
      return message === props.errorKey;
    } else {
      return (props.errorKey || []).indexOf(message) !== -1;
    }
  };

  let errors = (props.errorMessage || []).filter(message => compare(message));

  return (
    <div className={`form-group ${errors.length ? "has-error" : ""}`}>
      <div className={`col-md-12`}>
      <div className={`row`}>
        {props.children}

        {
          errors.map((err, index) =>
            <div key={index}>
              <label className="control-label col-md-offset-2 paddingLabelError">
                {errorManager.translate(err)}
              </label>
            </div>
          )
        }
      </div>
      </div>
    </div>
  );

};

FieldError.propTypes = {
  errorKey: PropTypes.oneOfType([PropTypes.array, PropTypes.string]),
  errorMessage: PropTypes.array
};
