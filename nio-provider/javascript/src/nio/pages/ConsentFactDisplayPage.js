import React, {Component} from 'react';
import PropTypes from "prop-types";
import Moment from 'react-moment';
import {ConsentsPage} from "./Consents";
import {LabelInput} from "../../common/ui/inputs";

export const ConsentFactDisplayPage = (props) => {

  const lastUpdate = props.user.lastUpdate ? <Moment locale="fr" parse="YYYY-MM-DDTHH:mm:ssZ"
                                                     format="DD/MM/YYYY HH:mm:ss">{props.user.lastUpdate}</Moment> : "NC";
  return (
    <div className="col-md-12">
      <div className="row">

        <div className="col-md-12">
          <h3>{props.customLabel || "Aperçu"}</h3>
        </div>

        <LabelInput value={props.organisationKey} label={"Rattaché à l'organisation"} alignLeft={true}/>
        <LabelInput value={props.user.version} label={"Basé sur la version"} alignLeft={true}/>
        <LabelInput value={lastUpdate} label={"Mis à jour le"} alignLeft={true}/>

        <ConsentsPage {...props} groups={props.user.groups} user={props.user}
                      submitable={false} readOnlyMode={true}/>
      </div>
    </div>
  );
};

ConsentFactDisplayPage.propTypes = {
  user: PropTypes.object.isRequired,
  customLabel: PropTypes.string,
  organisationKey: PropTypes.string.isRequired,
};