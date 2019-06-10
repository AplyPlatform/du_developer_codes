/*
 * Copyright (c) 2014 Samsung Electronics Co., Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following disclaimer
 *       in the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

var SAAgent = null;
var SAMessage = null;
var SAPeerAgent = null;
var ProviderAppName = "DronePlayerProvider";

function createHTML(log_string)
{
	console.log(log_string);
	var content = document.getElementById("toast-content");
	content.textContent = log_string;
	tau.openPopup("#toast");
}

function onerror(err) {
	console.log("err [" + err + "]");
}

function messageReceivedCallback(peerAgent, data) {
	createHTML("Received message from the [" + peerAgent.peerId + "] : " + data);
}

var peerAgentFindCallback = {
	onpeeragentfound : function(peerAgent) {
		try {
			if (peerAgent != null) console.log("App name is :" + peerAgent.appName);
			
			if (peerAgent.appName == ProviderAppName) {
				SAPeerAgent = peerAgent;
				for (var i = 0; i < peerAgent.feature.length; i++ )
				{
					console.log("peerAgent.feature :" + peerAgent.feature[i]);
				}

				if (peerAgent.feature.filter(function(v){return v === "MESSAGE";}) == "MESSAGE")
				{
					SAMessage = SAAgent.getSAMessage();
	                if(SAMessage)
	                {
	                	createHTML("PeerAgent Found!!");
	                	console.log("setMessageReceiveListener");
	                	SAMessage.setMessageReceiveListener(messageReceivedCallback);
	                }
				} else {
					createHTML("Not Support MESSAGE");
				}
			} else {
				console.log("Not expected app!! : " + peerAgent.appName);
			}
		} catch(err) {
			console.log("exception [" + err.name + "] msg[" + err.message + "]");
		}
	},
	onerror : onerror
}

function onsuccess(agents) {
	try {
		if (agents.length > 0) {
			SAAgent = agents[0];
			SAAgent.setPeerAgentFindListener(peerAgentFindCallback);
		} else {
			createHTML("Not found SAAgent!!");
		}
	} catch(err) {
		console.log("exception [" + err.name + "] msg[" + err.message + "]");
	}
}

function sendMessage() {
	if (SAPeerAgent) {
		if (SAMessage) {
			try {
				var id = SAMessage.sendData(SAPeerAgent, "Hello Message!", {
					onsent : function(peerAgent, id) { createHTML (id + " message is successfully sent to " + peerAgent.appName );},
					onerror : function(code, peerAgent, id) { console.log (id + " message is sent fail... code :" + code + "peerAgent name + " + peerAgent.appName); }
				});
				console.log("id : " + id);
			} catch(err) {
				console.log("exception [" + err.name + "] msg[" + err.message + "]");
			}
		} else {
			console.log("SAMessageObj is NULL");
		}
	} else {
		console.log("peer agent is NULL");
	}
}

function findPeerAgent()
{
	if (!SAAgent) {
		createHTML('SAAgent is NULL!');
        return false;
    } else {
    	SAAgent.findPeerAgents();
    }
}

function onreceive(channelId, data) {
	createHTML(data);
}

window.onload = function () {
    // add eventListener for tizenhwkey
    document.addEventListener('tizenhwkey', function(e) {
        if(e.keyName == "back")
            tizen.application.getCurrentApplication().exit();
    });

	try {
		webapis.sa.requestSAAgent(onsuccess, function (err) {
			console.log("err [" + err.name + "] msg[" + err.message + "]");
		});
	} catch(err) {
		console.log("exception [" + err.name + "] msg[" + err.message + "]");
	}
};

(function(tau) {
	var toastPopup = document.getElementById('toast');
	toastPopup.addEventListener('popupshow', function(ev){
		setTimeout(function(){tau.closePopup();}, 2000);
	}, false);
})(window.tau);