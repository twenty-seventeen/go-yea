/**
 * Copyright 2017 伊永飞
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.team.goyea.authorization.model.pk;


import com.yea.core.base.entity.BasePK;
import com.yea.core.base.id.RandomIDGennerator;

public class PersonInfoPK extends BasePK {

    private static final long serialVersionUID = 1L;
    
    private Long partyId;
    
    public PersonInfoPK() {
    }
    
    public PersonInfoPK(Long partyId) {
	    this.partyId = partyId;
    }

    public void setPartyId(Long partyId) {
        this.partyId = partyId;
    }
    public Long getPartyId() {
        return this.partyId;
    }
    
    public boolean isEmptyPK() {
        return
	          (partyId == null)
	       
               ? true : false;
    }
    
    public PersonInfoPK generatePK() {
        return new PersonInfoPK(
	            RandomIDGennerator.get().generate()
	       
                );
    }
}
